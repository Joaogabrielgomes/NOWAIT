export default function StarRating({ valor = 0, onChange, tamanho = '1.3rem', somenteLeitura = false }) {
  const estrelas = [1, 2, 3, 4, 5]

  return (
    <span className="star-rating" style={{ fontSize: tamanho }}>
      {estrelas.map((n) => (
        <span
          key={n}
          className={`star ${n <= Math.round(valor) ? 'filled' : ''} ${somenteLeitura ? '' : 'interactive'}`}
          onClick={somenteLeitura ? undefined : () => onChange?.(n)}
        >
          ★
        </span>
      ))}
    </span>
  )
}
