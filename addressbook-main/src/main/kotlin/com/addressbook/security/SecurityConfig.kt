package com.addressbook.security 

import org.springframework.beans.factory.annotation.Autowired 
import org.springframework.context.annotation.Configuration 
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder 
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity 
import org.springframework.security.config.annotation.web.builders.HttpSecurity 
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity 
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter 
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler 

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SecurityConfig: WebSecurityConfigurerAdapter() {

    @Autowired
    var  restAuthenticationEntryPoint: RestAuthenticationEntryPoint? = null

    @Autowired
    var mySuccessHandler: RequestAwareAuthenticationSuccessHandler? = null

    @Autowired
    var  igniteAuthenticationProvider: IgniteAuthenticationProvider ?= null

    val myFailureHandler = SimpleUrlAuthenticationFailureHandler()

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.authenticationProvider(igniteAuthenticationProvider) 
    }

    override fun configure(http: HttpSecurity) {
        http
                .csrf().disable()
                .exceptionHandling()
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .and()
                .authorizeRequests()
                .antMatchers("/rest/admin/**").hasRole("ADMIN")
                .antMatchers("/rest/**").hasRole("USER")
                .and()
                .formLogin()
                .successHandler(mySuccessHandler)
                .failureHandler(myFailureHandler)
                .and()
                .logout() 
    }
}