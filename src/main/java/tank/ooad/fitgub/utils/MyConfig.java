package tank.ooad.fitgub.utils;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import tank.ooad.fitgub.utils.permission.RequireLoginInterceptor;

@Configuration
public class MyConfig extends WebMvcConfigurerAdapter {

    public static final String HOST = "127.0.0.1";
    public static final String GIT_HTTP_SERVER_BASE = "https://ooad.dgy.ac.cn/git";

    public static final String PAGE_HTTP_SERVER_BASE = "https://ooad.ac.cn/pages";
    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(new RequireLoginInterceptor());
    }
}