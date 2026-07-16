"use client"

import { Component, type ReactNode } from "react"

interface Props { children: ReactNode }
interface State { error: Error | null }

export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error) {
    return { error }
  }

  render() {
    if (this.state.error) {
      return (
        <div className="h-dvh flex flex-col items-center justify-center p-6 bg-background text-center">
          <p className="text-destructive font-bold text-lg mb-2">Error</p>
          <p className="text-sm text-muted-foreground mb-4">
            {this.state.error.message}
          </p>
          <button
            onClick={() => { this.setState({ error: null }); window.location.reload() }}
            className="px-6 h-10 rounded-xl bg-primary text-white text-sm font-medium"
          >
            Reintentar
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
