package com.tianji.common.autoconfigure.mvc.advice;

import com.tianji.common.constants.Constant;
import com.tianji.common.domain.R;
import com.tianji.common.utils.WebUtils;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class WrapperResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    /*
    Allows customizing the response after the execution of
    an @ResponseBody or a ResponseEntity controller method
    but before the body is written with an HttpMessageConverter.

    Implementations may be registered directly with
    RequestMappingHandlerAdapter and ExceptionHandlerExceptionResolver
    or more likely annotated with @ControllerAdvice in which case they will be auto-detected by both.

    在结果被MessageConverter转换前（通常是对象转成JSON前），对结果进行修改。
    给这个类加上@ControllerAdvice注解。
     */

    //这个方法如果返回true，则需要执行下面的beforeBodyWrite()方法来对结果进行修改。
    @Override
    public boolean supports(MethodParameter returnType, //
                            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        //只有在结果不等于R（即通用结果类）且该请求是直接由路由节点转发过来的才进行装换
        return returnType.getParameterType() != R.class && WebUtils.isGatewayRequest();
    }

    //这个方法对结果进行修改
    @Override
    public Object beforeBodyWrite(
            Object body, @NonNull MethodParameter returnType, @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        if (request.getURI().getPath().equals("/v2/api-docs")){
            return body;
        }
        if (body == null) {
            return R.ok().requestId(MDC.get(Constant.REQUEST_ID_HEADER));
        }
        if(body instanceof R){
            return body;
        }
        return R.ok(body).requestId(MDC.get(Constant.REQUEST_ID_HEADER));
    }
}
