describe('Executions', () => {
  beforeEach(() => {
    cy.mockApi()
    cy.visit('/executions')
  })

  it('should display the executions list', () => {
    cy.wait('@getExecutions')
    
    cy.contains('Workflow Executions').should('be.visible')
    cy.get('[data-cy="execution-row"]').should('have.length.at.least', 1)
  })

  it('should show execution status badges', () => {
    cy.wait('@getExecutions')
    
    cy.contains('Completed').should('be.visible')
    cy.contains('Failed').should('be.visible')
    cy.contains('In Progress').should('be.visible')
  })

  it('should filter by status', () => {
    cy.wait('@getExecutions')
    
    // Click on status filter
    cy.get('[data-cy="status-filter"]').click()
    cy.get('[data-cy="status-option-FAILED"]').click()
    
    cy.wait('@getExecutions')
  })

  it('should navigate to execution detail', () => {
    cy.wait('@getExecutions')
    cy.mockExecution('exec-002')
    
    cy.get('[data-cy="execution-row"]').first().click()
    
    cy.wait('@getExecution')
    cy.url().should('include', '/executions/')
  })

  it('should paginate results', () => {
    cy.intercept('GET', '/api/workflows*', {
      fixture: 'executions.json',
      headers: { 'x-total-pages': '5' }
    }).as('getPagedExecutions')
    
    cy.visit('/executions')
    cy.wait('@getPagedExecutions')
    
    // Check pagination exists
    cy.get('[data-cy="pagination"]').should('exist')
  })
})
