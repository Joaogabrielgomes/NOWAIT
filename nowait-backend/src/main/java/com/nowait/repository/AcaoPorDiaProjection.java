package com.nowait.repository;

import java.time.LocalDate;

public interface AcaoPorDiaProjection {

    LocalDate getDia();

    String getAcao();

    Long getQuantidade();
}
