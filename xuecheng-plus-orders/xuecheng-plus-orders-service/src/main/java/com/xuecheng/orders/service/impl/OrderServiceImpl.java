package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author LH
 * @version 1.0
 * @description 订单相关接口
 * @date 2024/09/12 10:12
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    XcOrdersMapper ordersMapper;

    @Autowired
    XcOrdersGoodsMapper ordersGoodsMapper;

    @Autowired
    XcPayRecordMapper payRecordMapper;

    @Autowired
    OrderServiceImpl currentProxy;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    MqMessageService mqMessageService;

    @Value("${pay.qrcodeurl}")
    String qrcodeUrl;

    @Value("${pay.alipay.APP_ID}")
    String APP_ID;

    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;

    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;


    @Transactional
    @Override
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {

        //插入订单表、订单主表、订单明细表
        XcOrders xcOrders = saveXcOrders(userId, addOrderDto);

        //插入支付记录
        XcPayRecord payRecord = createPayRecord(xcOrders);
        Long payNo = payRecord.getPayNo();

        //生成二维码
        QRCodeUtil qrCodeUtil = new QRCodeUtil();
        //二维码图片
        String qrCode = null;
        //支付二维码的url
        String url = String.format(qrcodeUrl, payNo);
        try {
            qrCode = qrCodeUtil.createQRCode(url, 200, 200);
        } catch (IOException e) {
            XueChengPlusException.cast("生成二维码出错");
        }

        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        payRecordDto.setQrcode(qrCode);

        return payRecordDto;
    }

    /**
     * 保存订单信息
     *
     * @param userId
     * @param addOrderDto
     * @return
     */
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto) {
        //进行幂等性判断，同一个选课记录只能有一个订单
        XcOrders xcOrders = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if (xcOrders != null) {
            return xcOrders;
        }

        //插入订单主表
        xcOrders = new XcOrders();
        //使用雪花算法生成订单号
        xcOrders.setId(IdWorkerUtils.getInstance().nextId());
        xcOrders.setTotalPrice(addOrderDto.getTotalPrice());
        xcOrders.setCreateDate(LocalDateTime.now());
        xcOrders.setStatus("600001");//未支付
        xcOrders.setUserId(userId);
        xcOrders.setOrderType("60201");//订单类型
        xcOrders.setOrderName(addOrderDto.getOrderName());
        xcOrders.setOrderDescrip(addOrderDto.getOrderDescrip());
        xcOrders.setOrderDetail(addOrderDto.getOrderDetail());
        xcOrders.setOutBusinessId(addOrderDto.getOutBusinessId());//如果是选课这里记录选课表的id
        int insert = ordersMapper.insert(xcOrders);
        if (insert <= 0) {
            XueChengPlusException.cast("添加订单失败");
        }
        //订单id
        Long orderId = xcOrders.getId();

        //插入订单明细表
        //将前端传入的明细json串转为List
        String orderDetailJson = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> xcOrdersGoods = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        //遍历xcOrdersGoods插入订单明细
        xcOrdersGoods.forEach(goods -> {
            goods.setOrderId(orderId);
            //插入订单明细表
            int insert1 = ordersGoodsMapper.insert(goods);
        });

        return xcOrders;
    }

    /**
     * 根据业务id查询订单，业务id是选课记录表中的主键
     *
     * @param businessId
     * @return
     */
    public XcOrders getOrderByBusinessId(String businessId) {
        XcOrders orders = ordersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));
        return orders;
    }

    /**
     * 保存支付记录
     *
     * @param orders
     * @return
     */
    public XcPayRecord createPayRecord(XcOrders orders) {
        //订单id
        Long orderId = orders.getId();
        XcOrders xcOrders = ordersMapper.selectById(orderId);
        //如果此订单不存在不能添加支付记录
        if (xcOrders == null) {
            XueChengPlusException.cast("订单不存在");
        }
        //订单状态
        String status = xcOrders.getStatus();
        //如果此订单支付结果为成功，不在添加支付记录，避免重复支付
        if ("601002".equals(status)) {
            XueChengPlusException.cast("订单已支付");
        }

        //添加支付记录
        XcPayRecord xcPayRecord = new XcPayRecord();
        xcPayRecord.setPayNo(IdWorkerUtils.getInstance().nextId());//支付记录号，将来要传给支付宝
        xcPayRecord.setOrderId(orderId);
        xcPayRecord.setOrderName(xcOrders.getOrderName());
        xcPayRecord.setTotalPrice(xcOrders.getTotalPrice());
        xcPayRecord.setCurrency("CNY");
        xcPayRecord.setCreateDate(LocalDateTime.now());
        xcPayRecord.setStatus("601001");//未支付
        xcPayRecord.setUserId(xcOrders.getUserId());
        int insert = payRecordMapper.insert(xcPayRecord);
        if (insert <= 0) {
            XueChengPlusException.cast("插入支付记录失败");
        }

        return xcPayRecord;
    }

    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        XcPayRecord xcPayRecord = payRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
        return xcPayRecord;
    }

    @Override
    public PayRecordDto queryPayResult(String payNo) {
        //调用支付宝的接口查询支付结果
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);
        System.out.println(payStatusDto);

        //拿到支付结果更新支付记录表和订单表的支付状态
        currentProxy.saveAliPayStatus(payStatusDto);

        //要返回最新的支付记录信息
        XcPayRecord payRecordByPayno = getPayRecordByPayno(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecordByPayno, payRecordDto);

        return payRecordDto;
    }

    /**
     * 请求支付宝查询支付结果
     *
     * @param payNo 支付交易号
     * @return 支付结果
     */
    public PayStatusDto queryPayResultFromAlipay(String payNo) {
        //========请求支付宝查询支付结果=============
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, "json", AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE); //获得初始化的AlipayClient
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                XueChengPlusException.cast("请求支付宝查询支付结果失败");
            }
        } catch (AlipayApiException e) {
            log.error("请求支付宝查询支付结果异常:{}", e.toString(), e);
            XueChengPlusException.cast("请求支付查询查询失败");
        }

        //获取支付结果
        String resultJson = response.getBody();
        //转map
        Map resultMap = JSON.parseObject(resultJson, Map.class);
        Map alipay_trade_query_response = (Map) resultMap.get("alipay_trade_query_response");
        //支付结果
        String trade_status = (String) alipay_trade_query_response.get("trade_status");
        String total_amount = (String) alipay_trade_query_response.get("total_amount");
        String trade_no = (String) alipay_trade_query_response.get("trade_no");
        //保存支付结果
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_status(trade_status);//交易状态
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTrade_no(trade_no);
        payStatusDto.setTotal_amount(total_amount);//总金额

        return payStatusDto;
    }

    /**
     * @param payStatusDto 支付结果信息，从支付宝中查询的信息
     * @return void
     * @description 保存支付宝支付结果
     * @author Mr.M
     * @date 2022/10/4 16:52
     */
    @Transactional
    public void saveAliPayStatus(PayStatusDto payStatusDto) {
        //支付的记录号
        String payNo = payStatusDto.getOut_trade_no();
        XcPayRecord payRecordByPayno = getPayRecordByPayno(payNo);
        if (payRecordByPayno == null) {
            XueChengPlusException.cast("找不到相关的支付记录");
        }
        //拿到相关联的订单id
        Long orderId = payRecordByPayno.getOrderId();
        XcOrders xcOrders = ordersMapper.selectById(orderId);
        if (xcOrders == null) {
            XueChengPlusException.cast("找不到相关联的订单");
        }
        //支付状态
        String statusFromDb = payRecordByPayno.getStatus();
        //如果数据库支付的状态已经是成功了，不在处理了
        if ("601002".equals(statusFromDb)) {
            //如果已经成功了
            return;
        }

        //如果支付成功
        String trade_status = payStatusDto.getTrade_status();//支付宝返回的信息为支付成功
        if (trade_status.equals("TRADE_SUCCESS")) {//支付宝返回的信息为成功
            //更新支付记录表的状态为支付成功
            payRecordByPayno.setStatus("601002");
            //支付宝的订单号
            payRecordByPayno.setOutPayNo(payStatusDto.getTrade_no());
            //第三方支付渠道编号
            payRecordByPayno.setOutPayChannel("Alipay");
            //支付成功的时间
            payRecordByPayno.setPaySuccessTime(LocalDateTime.now());
            payRecordMapper.updateById(payRecordByPayno);


            //更新订单表的状态为支付成功
            xcOrders.setStatus("600002");//订单状态为交易成功
            ordersMapper.updateById(xcOrders);

            //将消息写到数据库
            MqMessage mqMessage = mqMessageService.addMessage("payresult_notify", xcOrders.getOutBusinessId(), xcOrders.getOrderType(), null);
            //发送消息
            notifyPayResult(mqMessage);
        }
    }

    @Override
    public void notifyPayResult(MqMessage message) {
        //消息内容
        String jsonString = JSON.toJSONString(message);
        //创建一个持久化消息
        Message messageObj = MessageBuilder.withBody(jsonString.getBytes(StandardCharsets.UTF_8)).setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();

        //消息id
        Long id = message.getId();
        //全局消息id
        CorrelationData correlationData = new CorrelationData();
        //使用correlationData指定回调方法
        correlationData.getFuture().addCallback(result -> {
            if (result.isAck()) {
                //消息成功发送到交换机
                log.debug("发送消息成功:{}", jsonString);
                //将消息从数据库表mq_message删除
                mqMessageService.completed(id);
            } else {
                //消息发送失败
                log.debug("发送消息失败:{}", jsonString);
            }
        }, ex -> {
            //发生异常了
            log.debug("发送消息异常:{}", jsonString);
        });

        //发送消息
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT, "", messageObj, correlationData);
    }
}
