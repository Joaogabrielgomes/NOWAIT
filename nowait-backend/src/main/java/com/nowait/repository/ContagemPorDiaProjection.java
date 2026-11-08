package com.nowait.repository;

import java.time.LocalDate;

public interface ContagemPorDiaProjection {

    LocalDate getDia();

    Long getQuantidade();
}
