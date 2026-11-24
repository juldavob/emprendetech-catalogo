package com.emprendetech.catalogo.presentacion;

import com.emprendetech.shared_kernel.excepcion.ConflictoException;
import com.emprendetech.shared_kernel.excepcion.DatoInvalidoException;
import com.emprendetech.shared_kernel.excepcion.NoEncontradoException;
import com.emprendetech.shared_kernel.excepcion.OperacionNoPermitidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce las excepciones del dominio a respuestas HTTP (RFC 9457, ProblemDetail).
 * Los errores de validación y de formato de Spring MVC ya los resuelve la clase base (400).
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NoEncontradoException.class)
    public ProblemDetail noEncontrado(NoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ProblemDetail prohibido(OperacionNoPermitidaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(DatoInvalidoException.class)
    public ProblemDetail datoInvalido(DatoInvalidoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ProblemDetail conflicto(ConflictoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
