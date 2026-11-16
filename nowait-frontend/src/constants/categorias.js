export const CATEGORIAS_ESTABELECIMENTO = [
  { valor: 'LANCHES', rotulo: 'Lanches' },
  { valor: 'PIZZA', rotulo: 'Pizza' },
  { valor: 'ITALIANA', rotulo: 'Italiana' },
  { valor: 'JAPONESA', rotulo: 'Japonesa' },
  { valor: 'BRASILEIRA', rotulo: 'Brasileira' },
  { valor: 'MARMITA', rotulo: 'Marmita' },
  { valor: 'CHURRASCO', rotulo: 'Churrasco' },
  { valor: 'FRUTOS_DO_MAR', rotulo: 'Frutos do mar' },
  { valor: 'VEGETARIANA', rotulo: 'Vegetariana' },
  { valor: 'CAFETERIA', rotulo: 'Cafeteria' },
  { valor: 'DOCES_SOBREMESAS', rotulo: 'Doces e sobremesas' },
  { valor: 'ARABE', rotulo: 'Árabe' },
  { valor: 'MEXICANA', rotulo: 'Mexicana' },
  { valor: 'OUTRO', rotulo: 'Outro' },
]

export function rotuloCategoria(valor) {
  return CATEGORIAS_ESTABELECIMENTO.find((c) => c.valor === valor)?.rotulo || valor
}
