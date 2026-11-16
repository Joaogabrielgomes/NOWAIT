package com.nowait.model;

public final class CategoriaEstabelecimento {

    public static final String LANCHES = "LANCHES";
    public static final String PIZZA = "PIZZA";
    public static final String ITALIANA = "ITALIANA";
    public static final String JAPONESA = "JAPONESA";
    public static final String BRASILEIRA = "BRASILEIRA";
    public static final String MARMITA = "MARMITA";
    public static final String CHURRASCO = "CHURRASCO";
    public static final String FRUTOS_DO_MAR = "FRUTOS_DO_MAR";
    public static final String VEGETARIANA = "VEGETARIANA";
    public static final String CAFETERIA = "CAFETERIA";
    public static final String DOCES_SOBREMESAS = "DOCES_SOBREMESAS";
    public static final String ARABE = "ARABE";
    public static final String MEXICANA = "MEXICANA";
    public static final String OUTRO = "OUTRO";

    public static final String TODAS =
            LANCHES + "|" + PIZZA + "|" + ITALIANA + "|" + JAPONESA + "|" + BRASILEIRA + "|" + MARMITA + "|"
                    + CHURRASCO + "|" + FRUTOS_DO_MAR + "|" + VEGETARIANA + "|" + CAFETERIA + "|"
                    + DOCES_SOBREMESAS + "|" + ARABE + "|" + MEXICANA + "|" + OUTRO;

    private CategoriaEstabelecimento() {
    }
}
