package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
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
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrdersServiceImplx implements OrdersService {
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


    @Override
    @Transactional //该方法涉及多张表的插入等操作需要开启事务
    public OrderSubmitVO submit(OrdersSubmitDTO dto) {
        //1.构造订单表并补充相应的字段
        Orders orders = new Orders();
        BeanUtils.copyProperties(dto, orders);

        Long currentId = BaseContext.getCurrentId();
        //1.1设置订单号和付款状态以及用户id
        orders.setNumber(System.currentTimeMillis() + "" + currentId);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(currentId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);

        //1.2 查询获取地址簿相关信息
        AddressBook addressBook = addressBookMapper.getById(dto.getAddressBookId());
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        orders.setPhone(addressBook.getPhone());
        orders.setAddress(addressBook.getDetail());
        orders.setConsignee(addressBook.getConsignee());

        //1.3查询用户表
        User user=userMapper.getById(currentId);
        if (user==null){
            throw new UserNotLoginException(MessageConstant.USER_NOT_LOGIN);
        }
        orders.setUserName(user.getName());

        //1.4插入订单表
        ordersMapper.insert(orders);
        log.info("订单表插入成功，订单id为：{}",orders.getId());

        //2.构造订单明细表并补充相应的字段
        List<OrderDetail> orderDetailList=new ArrayList<>();
        //2.1查询当前用户的购物车数据
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

        //2.2批量插入订单明细表
        orderDetailMapper.insertBatch(orderDetailList);

        //3.清空购物车

        shoppingCartMapper.clean(currentId);
        //4.构造并返回订单提交结果VO


        return  new OrderSubmitVO().builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
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
}
