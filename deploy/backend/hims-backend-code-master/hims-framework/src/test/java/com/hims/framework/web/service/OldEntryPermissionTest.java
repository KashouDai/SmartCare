package com.hims.framework.web.service;

import com.hims.common.constant.Constants;
import com.hims.common.core.domain.model.LoginUser;
import com.hims.common.exception.ServiceException;
import com.hims.info.controller.AppointmentsInfoController;
import com.hims.info.domain.AppointmentsInfo;
import com.hims.order.controller.OrderController;
import com.hims.order.domain.Order;
import com.hims.order.domain.RegisteredOrderRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 阶段 2.5 旧入口收权的权限层验证。
 *
 * 覆盖两件事：
 * 1. 两个旧入口的 {@code @PreAuthorize} 注解存在且权限串正确（此前这两个注解被注释掉，
 *    任何登录用户都能绕过统一事务直接建预约/建订单，属回归高发点）
 * 2. {@code PermissionService.hasPermi} 在「普通角色权限集为空」时拒绝、在管理员
 *    {@code *:*:*} 时放行，即收权挡住患者端账号且不产生管理端功能回归
 *
 * 本测试不启动 Spring 容器，因此**不能**证明 {@code @ss} Bean 已注册、
 * 也不能证明真实 JWT 过滤器 + 方法安全切面在运行时生效；那需要集成测试或运行中的服务。
 */
class OldEntryPermissionTest
{
    private static final String APPOINTMENTS_ADD_PERMISSION = "info:appointments:add";
    private static final String ORDER_ADD_PERMISSION = "order:orderInfo:add";

    private final PermissionService permissionService = new PermissionService();

    /**
     * {@code PermissionService.hasPermi} 在权限集非空时会把权限串写入请求作用域属性
     * （{@code PermissionContextHolder}，供操作日志记录），因此必须绑定一个请求上下文，
     * 否则抛 IllegalStateException。这里用 MockHttpServletRequest 模拟，未走真实 servlet 容器。
     */
    @BeforeEach
    void bindRequestScope()
    {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void clearContext()
    {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void 旧预约入口_注解存在且权限串正确() throws NoSuchMethodException
    {
        PreAuthorize annotation = AppointmentsInfoController.class
                .getMethod("add", AppointmentsInfo.class)
                .getAnnotation(PreAuthorize.class);

        assertNotNull(annotation, "POST /info/appointments 必须保留 @PreAuthorize："
                + "该接口使用非原子占号、无患者归属校验，且预约和订单不在统一事务中，"
                + "可绕过统一下单链路，放开会给非管理端留下绕过统一事务的写入路径");
        assertEquals("@ss.hasPermi('info:appointments:add')", annotation.value());
    }

    @Test
    void 旧订单入口_注解存在且权限串正确() throws NoSuchMethodException
    {
        PreAuthorize annotation = OrderController.class
                .getMethod("add", Order.class)
                .getAnnotation(PreAuthorize.class);

        assertNotNull(annotation, "POST /order/orderInfo 必须保留 @PreAuthorize："
                + "放开后任何登录用户都能建订单");
        assertEquals("@ss.hasPermi('order:orderInfo:add')", annotation.value());
    }

    @Test
    void 统一下单入口_只要求登录_不要求管理端权限() throws NoSuchMethodException
    {
        PreAuthorize annotation = OrderController.class
                .getMethod("registeredOrder", RegisteredOrderRequest.class)
                .getAnnotation(PreAuthorize.class);

        assertNotNull(annotation, "患者端下单依赖该注解声明匿名/登录边界，缺失会改变访问语义");
        assertEquals("isAuthenticated()", annotation.value(),
                "患者端账号没有管理端权限，统一下单入口不能要求 hasPermi");
    }

    @Test
    void 普通角色_权限集为空_两个旧入口均被拒绝()
    {
        loginWithPermissions();

        assertFalse(permissionService.hasPermi(APPOINTMENTS_ADD_PERMISSION),
                "普通角色权限集为空时旧预约入口必须被拒绝");
        assertFalse(permissionService.hasPermi(ORDER_ADD_PERMISSION),
                "普通角色权限集为空时旧订单入口必须被拒绝");
    }

    @Test
    void 普通角色_仅持有订单新增权限_预约入口仍被拒绝()
    {
        loginWithPermissions(ORDER_ADD_PERMISSION);

        assertTrue(permissionService.hasPermi(ORDER_ADD_PERMISSION));
        assertFalse(permissionService.hasPermi(APPOINTMENTS_ADD_PERMISSION),
                "两个入口的权限串不同，持有其中一个不能顶替另一个");
    }

    @Test
    void 管理员_权限集含通配_两个旧入口均放行()
    {
        loginWithPermissions(Constants.ALL_PERMISSION);

        assertTrue(permissionService.hasPermi(APPOINTMENTS_ADD_PERMISSION),
                "管理员走 *:*:*，收权不应造成管理端新增预约的功能回归");
        assertTrue(permissionService.hasPermi(ORDER_ADD_PERMISSION),
                "管理员走 *:*:*，收权不应造成管理端新增订单的功能回归");
    }

    @Test
    void 未登录_权限判定抛出未授权异常()
    {
        SecurityContextHolder.clearContext();

        assertThrows(ServiceException.class,
                () -> permissionService.hasPermi(APPOINTMENTS_ADD_PERMISSION),
                "无登录态必须抛出未授权异常，不能静默放行");
    }

    private void loginWithPermissions(String... permissions)
    {
        Set<String> granted = new HashSet<>();
        Collections.addAll(granted, permissions);
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(100L);
        loginUser.setPermissions(granted);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }
}
