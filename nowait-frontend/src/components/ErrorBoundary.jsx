import { Component } from 'react'

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  componentDidCatch(error, info) {
    console.error('Erro não tratado na interface:', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="container">
          <div className="error-box">
            Ocorreu um erro inesperado na aplicação. Recarregue a página.
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
