describe('Dashboard', () => {
  beforeEach(() => {
    cy.mockApi()
    cy.visit('/')
  })

  it('should display the dashboard page', () => {
    cy.wait('@getDashboard')
    cy.contains('Dashboard').should('be.visible')
  })

  it('should show workflow statistics', () => {
    cy.wait('@getDashboard')
    
    // Check stats cards are displayed
    cy.contains('Completed').should('be.visible')
    cy.contains('Failed').should('be.visible')
    cy.contains('In Progress').should('be.visible')
  })

  it('should display recent executions', () => {
    cy.wait('@getDashboard')
    
    cy.contains('Recent Executions').should('be.visible')
    cy.contains('exec-001').should('be.visible')
    cy.contains('order-workflow').should('be.visible')
  })

  it('should navigate to executions page', () => {
    cy.wait('@getDashboard')
    
    cy.get('[data-cy="nav-executions"]').click()
    cy.url().should('include', '/executions')
  })

  it('should navigate to metrics page', () => {
    cy.wait('@getDashboard')
    
    cy.get('[data-cy="nav-metrics"]').click()
    cy.url().should('include', '/metrics')
  })
})
