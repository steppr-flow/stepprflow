describe('Metrics', () => {
  beforeEach(() => {
    cy.mockApi()
    cy.visit('/metrics')
  })

  it('should display metrics page', () => {
    cy.wait('@getMetrics')
    cy.contains('Metrics').should('be.visible')
  })

  it('should show global statistics', () => {
    cy.wait('@getMetrics')
    
    cy.contains('Total Started').should('be.visible')
    cy.contains('Total Completed').should('be.visible')
    cy.contains('Success Rate').should('be.visible')
  })

  it('should show workflow metrics table', () => {
    cy.wait('@getMetrics')
    
    cy.contains('order-workflow').should('be.visible')
    cy.contains('payment-workflow').should('be.visible')
  })

  it('should display circuit breakers', () => {
    cy.wait('@getCircuitBreakers')
    
    cy.contains('Circuit Breakers').should('be.visible')
    cy.contains('order-workflow-cb').should('be.visible')
    cy.contains('CLOSED').should('be.visible')
    cy.contains('HALF_OPEN').should('be.visible')
  })

  it('should reset circuit breaker', () => {
    cy.wait('@getCircuitBreakers')
    
    cy.intercept('POST', '/api/circuit-breakers/*/reset', { statusCode: 200 }).as('resetCb')
    cy.intercept('GET', '/api/circuit-breakers', { fixture: 'circuitBreakers.json' }).as('refreshCb')
    
    cy.get('[data-cy="reset-cb-btn"]').first().click()
    cy.get('[data-cy="confirm-btn"]').click()
    
    cy.wait('@resetCb')
    cy.wait('@refreshCb')
  })

  it('should show health indicator based on success rate', () => {
    cy.wait('@getMetrics')
    
    // With 92.1% success rate, should show warning (between 80-95%)
    cy.get('[data-cy="health-indicator"]').should('have.class', 'warning')
  })
})
