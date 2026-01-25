package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.UserNotLoginException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrdersService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        if(orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
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
}
