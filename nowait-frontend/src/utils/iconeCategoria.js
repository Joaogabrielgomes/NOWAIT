import {
  Sandwich,
  Pizza,
  Soup,
  Fish,
  Beef,
  Utensils,
  Coffee,
  CakeSlice,
  Salad,
} from 'lucide-react'

const ICONES_POR_CATEGORIA = {
  LANCHES: Sandwich,
  PIZZA: Pizza,
  ITALIANA: Soup,
  JAPONESA: Fish,
  BRASILEIRA: Utensils,
  MARMITA: Utensils,
  CHURRASCO: Beef,
  FRUTOS_DO_MAR: Fish,
  VEGETARIANA: Salad,
  CAFETERIA: Coffee,
  DOCES_SOBREMESAS: CakeSlice,
  ARABE: Soup,
  MEXICANA: Soup,
  OUTRO: Utensils,
}

export function iconeCategoria(categoria) {
  return ICONES_POR_CATEGORIA[categoria] || Utensils
}
