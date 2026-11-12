package com.coletaseletiva.config;

import java.util.List;
import java.util.Locale;

import com.coletaseletiva.entity.TipoAtor;
import com.coletaseletiva.exception.EntradaInvalidaException;
import com.coletaseletiva.service.ContextoAtor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ContextoWebConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new ContextoAtorResolver());
    }

    private static class ContextoAtorResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parametro) {
            return parametro.getParameterType() == ContextoAtor.class;
        }

        @Override
        public Object resolveArgument(MethodParameter parametro, ModelAndViewContainer mav,
                                      NativeWebRequest request, WebDataBinderFactory binder) {
            String tipo = request.getHeader("X-Ator-Tipo");
            String id = request.getHeader("X-Ator-Id");
            return new ContextoAtor(lerTipo(tipo), lerId(id));
        }

        private TipoAtor lerTipo(String tipo) {
            if (tipo == null || tipo.isBlank()) {
                return null;
            }
            try {
                return TipoAtor.valueOf(tipo.strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new EntradaInvalidaException("X-Ator-Tipo", "Perfil de demonstração inválido.");
            }
        }

        private Long lerId(String id) {
            if (id == null || id.isBlank()) {
                return null;
            }
            try {
                long valor = Long.parseLong(id.strip());
                if (valor > 0) {
                    return valor;
                }
            } catch (NumberFormatException ignored) {
                // O erro é retornado no formato de validação da API.
            }
            throw new EntradaInvalidaException("X-Ator-Id", "Informe um identificador positivo.");
        }
    }
}
