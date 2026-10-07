package com.yike.coffee;

import com.yike.coffee.api.ApiException;
import com.yike.coffee.mapper.MerchantMemberMapper;
import com.yike.coffee.mapper.UserMapper;
import com.yike.coffee.security.JwtService;
import com.yike.coffee.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AuthServiceTest {
    @Test void productionDisablesDevLogin(){
        var service=new AuthService(mock(UserMapper.class),mock(MerchantMemberMapper.class),mock(JdbcTemplate.class),mock(JwtService.class),false,7);
        assertThatThrownBy(()->service.devLogin("admin@example.test")).isInstanceOf(ApiException.class).hasMessage("接口不存在");
    }
}
