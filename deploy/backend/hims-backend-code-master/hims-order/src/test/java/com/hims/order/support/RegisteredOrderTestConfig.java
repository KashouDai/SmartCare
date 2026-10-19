package com.hims.order.support;

import javax.sql.DataSource;

import com.hims.order.service.impl.OrderServiceImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootConfiguration
@EnableTransactionManagement
@MapperScan({"com.hims.order.mapper", "com.hims.info.mapper"})
public class RegisteredOrderTestConfig {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:registered_order_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        DatabasePopulatorUtils.execute(new ResourceDatabasePopulator(new ClassPathResource("registered_order_test_schema.sql")), dataSource);
        return dataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage("com.hims.order.domain,com.hims.info.domain");
        factoryBean.setMapperLocations(
                new ClassPathResource("mapper/order/OrderMapper.xml"),
                new ClassPathResource("mapper/order/AppointmentIdempotentMapper.xml"),
                new ClassPathResource("mapper/info/AppointmentsInfoMapper.xml"),
                new ClassPathResource("mapper/info/DutyInfoMapper.xml"),
                new ClassPathResource("mapper/info/PatientsInfoMapper.xml"),
                new ClassPathResource("mapper/info/UserPatientRelationMapper.xml"));
        return factoryBean.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public OrderServiceImpl orderService() {
        return new OrderServiceImpl();
    }
}
