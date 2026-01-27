package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.google.gson.JsonObject;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.UserNotLoginException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrdersService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrdersServiceImpl implements OrdersService {
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private DishMapper dishMapper;

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String baiduAk;


    /**
     * 用户下单
     */
    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO dto) {
        // 1 查询获取地址簿相关信息
        AddressBook addressBook = addressBookMapper.getById(dto.getAddressBookId());
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 拼接用户的完整地址：省 + 市 + 区 + 详细地址
        String userAddressStr = addressBook.getProvinceName()
                + addressBook.getCityName()
                + addressBook.getDistrictName()
                + addressBook.getDetail();

        // 调用校验逻辑
        checkOutOfRange(userAddressStr);


        //2.构造订单表并补充相应的字段
        Orders orders = new Orders();
        BeanUtils.copyProperties(dto, orders);

        Long currentId = BaseContext.getCurrentId();
        //2.1设置订单号和付款状态以及用户id
        orders.setNumber(System.currentTimeMillis() + "" + currentId);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(currentId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);


        orders.setPhone(addressBook.getPhone());
        orders.setAddress(addressBook.getDetail());
        orders.setConsignee(addressBook.getConsignee());

        //2.3查询用户表
        User user=userMapper.getById(currentId);
        if (user==null){
            throw new UserNotLoginException(MessageConstant.USER_NOT_LOGIN);
        }
        orders.setUserName(user.getName());

        //2.4插入订单表
        ordersMapper.insert(orders);
        log.info("订单表插入成功，订单id为：{}",orders.getId());

        //3.构造订单明细表并补充相应的字段
        List<OrderDetail> orderDetailList=new ArrayList<>();
        //3.1查询当前用户的购物车数据
        List<ShoppingCart> list = shoppingCartMapper.list(currentId);
        if(list==null || list.size()==0){
            throw new OrderBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //循环遍历
        list.forEach(cart->{
            OrderDetail orderDetail=new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail,"id");
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        });

        //3.2批量插入订单明细表
        orderDetailMapper.insertBatch(orderDetailList);

        //4.清空购物车

        shoppingCartMapper.clean(currentId);
        //5.构造并返回订单提交结果VO


        return  OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
    }

    /**
     * 检查是否超出配送范围
     * @param userAddressStr 用户完整地址
     */
    private void checkOutOfRange(String userAddressStr) {
        log.info("开始校验配送距离，商家地址：{}，用户地址：{}", shopAddress, userAddressStr);

        // 1. 调用百度地图地理编码接口，获取【商家】的经纬度
        // API URL: https://api.map.baidu.com/geocoding/v3
        // 参数: address=shopAddress, output=json, ak=baiduAk
        String shopCoordinate = getCoordinate(shopAddress);
        if (shopCoordinate == null) {
            throw new OrderBusinessException("商家地址解析失败");
        }

        // 2. 调用百度地图地理编码接口，获取【用户】的经纬度
        String userCoordinate = getCoordinate(userAddressStr);
        if (userCoordinate == null) {
            throw new OrderBusinessException("您的收货地址无法解析");
        }

        // 3. 调用百度地图骑行路线规划接口，计算距离
        // API URL: https://api.map.baidu.com/direction/v2/cycling
        // 参数: origin=shopCoordinate, destination=userCoordinate, ak=baiduAk
        Long distance = getDistance(shopCoordinate, userCoordinate);

        if(distance > 5000) { // 5000米
            throw new OrderBusinessException("超出配送范围，配送距离为：" + distance + "米");
        }

        log.info("配送距离校验通过，距离：{}米", distance);
    }

    /**
     * 辅助方法：调用Geocoding API获取经纬度
     * 返回格式示例："40.05,116.30" (纬度,经度)
     */
    private String getCoordinate(String address) {
        // TODO: 使用 HttpClientUtil 发送 GET 请求
        // 解析返回的 JSON，提取 location.lat 和 location.lng
        // 注意：百度API返回的可能是 lng(经度), lat(纬度)，但Direction API 通常要求 "lat,lng" 格式，需仔细看文档
        //1.构造参数
        HashMap<String,String> params=new HashMap<>();
        params.put("address",address);
        params.put("output","json");
        params.put("ak",baiduAk);

        String url="https://api.map.baidu.com/geocoding/v3";

        //2.发送请求
        String string = HttpClientUtil.doGet(url, params);

        //3.解析数据
        JSONObject jsonObject = JSON.parseObject(string);

        //先检查状态是否准确
        if(!"0".equals(jsonObject.getString("status"))){
            // 解析失败（比如AK不对，或地址不存在），建议抛出业务异常或返回 null
            throw new OrderBusinessException("地址解析失败");
        }

        // 解析 location
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");

        // 拼接返回 "lat,lng"
        String lat=location.getString("lat");
        String lng=location.getString("lng");


        return lat+","+lng;
    }

    /**
     * 辅助方法：调用Direction API获取骑行距离（米）
     */
    private Long getDistance(String origin, String destination) {
        // TODO: 使用 HttpClientUtil 发送 GET 请求
        // 解析 JSON，提取 routes[0].distance
        //1.构造参数
        HashMap<String,String> params=new HashMap<>();
        params.put("origin",origin);
        params.put("destination",destination);
        params.put("ak",baiduAk);

        String url="https://api.map.baidu.com/direction/v2/riding";

        //2.发送请求
        String string = HttpClientUtil.doGet(url, params);

        //3.解析数据
        JSONObject jsonObject = JSON.parseObject(string);

        //先检查状态是否准确
        if(!"0".equals(jsonObject.getString("status"))){
            // 解析失败（比如AK不对，或地址不存在），建议抛出业务异常或返回 null
            throw new OrderBusinessException("距离解析失败");
        }

        //解析distance
        JSONArray routes = jsonObject.getJSONObject("result").getJSONArray("routes");
        Long distance = routes.getJSONObject(0).getLong("distance");
        return distance;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
//        // 当前登录用户id
//        Long userId = BaseContext.getCurrentId();
//        User user = userMapper.getById(userId);
//
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));

        ////////////////////模拟支付成功
        //支付成功，修改订单状态
        paySuccess(ordersPaymentDTO.getOrderNumber());


        return new OrderPaymentVO();
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询当前用户的订单
        Orders ordersDB = ordersMapper.getByNumberAndUserId(outTradeNo, userId);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        ordersMapper.update(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult historyOrders(OrdersPageQueryDTO dto) {
        //1.取出后当前用户的id
        dto.setUserId(BaseContext.getCurrentId());
        //2.设置分页插件
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        //3.执行分页查询
        //注意：这里不能直接查询OrderVO，因为需要Orders与Order_Detail的列表进行拼接
        Page<Orders> page=ordersMapper.list(dto);
        //4.构造分页结果返回
        //4.1创建空的List集合
        List<OrderVO> list=new ArrayList<>();
        if(page!=null && !page.isEmpty()){
            //4.2遍历page中的数据往集合中补充元素
            for(Orders orders:page){
                //创建一个空的订单的对象往订单中拷贝对应的数据
                OrderVO orderVO=new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                //查询订单明细数据
                List<OrderDetail> orderDetailList=orderDetailMapper.getByOrderId(orders.getId());
                orderVO.setOrderDetailList(orderDetailList);
                //把订单对象添加到List集合中
                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(),list);
    }

    @Transactional(readOnly = true)
    @Override
    public OrderVO orderDetail(Long id) {
        //1.根据id查询订单表
        Orders orders= ordersMapper.getById(id);

        if(orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //2.构造OrderVO对象
        OrderVO vo= new OrderVO();
        BeanUtils.copyProperties(orders,vo);
        //3.根据订单id查询订单明细表

        List<OrderDetail> list=orderDetailMapper.getByOrderId(vo.getId());
        vo.setOrderDetailList(list);


        return vo;
    }

    @Override
    public void cancel(Long id) {
        //1.根据id查询订单表
        Orders orders=ordersMapper.getById(id);
        if(orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //【安全校验】校验订单是否属于当前用户
        Long currentId=BaseContext.getCurrentId();
        if(!orders.getUserId().equals(currentId)){
            throw new OrderBusinessException("无权操作此订单");
        }

        //2.判断订单状态是否可以取消（状态为1，2可被取消）
        if(orders.getStatus()>=Orders.CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders order=new Orders();
        order.setId(id);

        //3.处理退款逻辑
        if(orders.getPayStatus().equals(Orders.PAID)){
            /**
             * 由于没有微信商户号这里不不进行退款操作
             */

            //修改支付状态
            order.setPayStatus(Orders.REFUND);
        }

        //4.修改订单状态为已取消
        order.setStatus(Orders.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason("用户取消");
        ordersMapper.update(order);
    }

    @Override
    public void repetition(Long id) {
        //1.根据订单id查询相应的订单
        Orders orders=ordersMapper.getById(id);

        if(orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //安全校验
        Long currentId=BaseContext.getCurrentId();
        if(!orders.getUserId().equals(currentId)){
            throw new OrderBusinessException("无权操作此订单");
        }
        //2.根据订单id查询订单明细数据
        List<OrderDetail> orderDetailList=orderDetailMapper.getByOrderId(id);
        if(orderDetailList==null || orderDetailList.size()==0){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //3.清空购物车数据
        shoppingCartMapper.clean(currentId);
        //4.将订单明细数据批量插入到购物车表中
        for(OrderDetail detail:orderDetailList){
            ShoppingCart shoppingCart=new ShoppingCart();
            //拷贝数据忽略id以及orderId字段因为这两个字段在购物车表中没有意义
            BeanUtils.copyProperties(detail,shoppingCart,"id","orderId");
            //补充用户id字段以及创建时间字段
            shoppingCart.setUserId(currentId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }

    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO dto) {
        //1.设置分页插件
        PageHelper.startPage(dto.getPage(),dto.getPageSize());
        //2.执行分页查询
        Page<Orders> page= ordersMapper.list(dto);
        //3.安全检查
        long total = page.getTotal();
        List<Orders> ordersList = page.getResult();
        List<OrderVO> list=new ArrayList<>();
        if(ordersList!=null && !ordersList.isEmpty()){
            for(Orders orders:ordersList){
                OrderVO vo=new OrderVO();
                //4.拷贝属性
                BeanUtils.copyProperties(orders,vo);
                //5.查询订单明细数据
                List<OrderDetail> details = orderDetailMapper.getByOrderId(orders.getId());

                //6.补充数据
                if(details!=null && !details.isEmpty()){
                    String dishes = details.stream()
                            .map(x -> x.getName() + "*" + x.getNumber() + ";")
                            .collect(Collectors.joining());
                    vo.setOrderDishes(dishes);
                }
                list.add(vo);
            }
        }
        return new PageResult(total,list);

    }

    @Override
    public OrderStatisticsVO statistics() {

        return ordersMapper.getOrderStatistics();
    }

    // 方法定义
    private Orders getValidOrder(Long id) {
        Orders order = ordersMapper.getById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return order;
    }
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        //1.根据id查询订单
        Orders order = getValidOrder(ordersConfirmDTO.getId());

        // 2. 校验状态（必须是“待接单”才能接单）
        // 使用常量 Orders.TO_BE_CONFIRMED 增强可读性
        if (!Orders.TO_BE_CONFIRMED.equals(order.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 3. 修改订单状态为已接单
        Orders orders=Orders.builder()
                .id(order.getId())
                .status(Orders.CONFIRMED)
                .build();

        ordersMapper.update(orders);

    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 1. 加上事务
    public void rejection(OrdersRejectionDTO dto) {
        // 1. 根据id查询订单
        Orders order = getValidOrder(dto.getId());

        // 2. 校验状态
        if (!Orders.TO_BE_CONFIRMED.equals(order.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 3. 构建更新对象
        // 建议命名为 orderToUpdate 以区分原始查询出的 order
        Orders orderToUpdate = Orders.builder()
                .id(order.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(dto.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();

        // 4. 支付状态校验
        if (order.getPayStatus() == Orders.PAID) {
            // 模拟退款逻辑
            // log.info("模拟退款成功...");
            orderToUpdate.setPayStatus(Orders.REFUND);
        }

        // 5. 更新
        ordersMapper.update(orderToUpdate);
    }

    @Override
    public void adminCancel(OrdersCancelDTO dto) {
        //1.根据id查询订单表
        Orders orders=ordersMapper.getById(dto.getId());
        if(orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //2.判断订单状态是否可以取消（状态6,7不可被取消）
        if(orders.getStatus()>=Orders.CANCELLED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders order=new Orders();
        order.setId(dto.getId());

        //3.处理退款逻辑
        if(orders.getPayStatus().equals(Orders.PAID)){
            /**
             * 由于没有微信商户号这里不不进行退款操作
             */

            //修改支付状态
            order.setPayStatus(Orders.REFUND);
        }

        //4.修改订单状态为已取消
        order.setStatus(Orders.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(dto.getCancelReason());
        ordersMapper.update(order);
    }

    @Override
    public void delivery(Long id) {
        //1.根据id查询订单
        Orders orders = getValidOrder(id);
        //2.校验订单状态（必须是已接单才能派送）
        if(orders.getStatus()!=Orders.CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //3.修改订单状态为派送中
        Orders order=Orders.builder()
                .id(orders.getId())
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        ordersMapper.update(order);
    }

    @Override
    public void complete(Long id) {
        //1.根据id查询订单
        Orders orders = getValidOrder(id);

        //2.校验订单状态（必须是派送中才能完成订单）
        if(orders.getStatus()!=Orders.DELIVERY_IN_PROGRESS){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //3.修改订单状态为已完成
        Orders order=Orders.builder()
                .id(orders.getId())
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
        ordersMapper.update(order);
    }
}
