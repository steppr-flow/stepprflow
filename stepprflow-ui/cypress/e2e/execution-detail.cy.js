describe('Execution Detail', () => {
  const executionId = 'exec-002'

  beforeEach(() => {
    cy.mockApi()
    cy.mockExecution(executionId)
    cy.visit(`/executions/${executionId}`)
    cy.wait('@getExecution')
  })

  it('should display execution details', () => {
    cy.contains(executionId).should('be.visible')
    cy.contains('payment-workflow').should('be.visible')
    cy.contains('Failed').should('be.visible')
  })

  it('should show step history', () => {
    cy.contains('Step History').should('be.visible')
    cy.contains('Validate').should('be.visible')
    cy.contains('Reserve').should('be.visible')
    cy.contains('Process Payment').should('be.visible')
  })

  it('should show error information for failed execution', () => {
    cy.contains('Error Details').should('be.visible')
    cy.contains('Payment gateway timeout').should('be.visible')
  })

  it('should show payload data', () => {
    cy.contains('Payload').should('be.visible')
    cy.contains('paymentId').should('be.visible')
    cy.contains('PAY-456').should('be.visible')
  })

  describe('actions', () => {
    it('should resume failed execution', () => {
      cy.mockResume(executionId)
      cy.mockExecution(executionId)
      
      cy.get('[data-cy="resume-btn"]').click()
      
      // Confirm in modal if exists
      cy.get('[data-cy="confirm-btn"]').click()
      
      cy.wait('@resumeExecution')
    })

    it('should cancel execution', () => {
      // Change to IN_PROGRESS for cancel to be available
      cy.intercept('GET', `/api/workflows/${executionId}`, {
        ...require('../fixtures/execution.json'),
        status: 'IN_PROGRESS'
      }).as('getInProgressExecution')
      
      cy.visit(`/executions/${executionId}`)
      cy.wait('@getInProgressExecution')
      
      cy.mockCancel(executionId)
      
      cy.get('[data-cy="cancel-btn"]').click()
      cy.get('[data-cy="confirm-btn"]').click()
      
      cy.wait('@cancelExecution')
    })
  })

  describe('payload editing', () => {
    it('should open payload editor', () => {
      cy.get('[data-cy="edit-payload-btn"]').click()
      cy.get('[data-cy="payload-editor"]').should('be.visible')
    })

    it('should update payload field', () => {
      cy.intercept('PATCH', `/api/workflows/${executionId}/payload`, {
        statusCode: 200,
        fixture: 'execution.json'
      }).as('updatePayload')
      
      cy.get('[data-cy="edit-payload-btn"]').click()
      cy.get('[data-cy="payload-field-amount"]').clear().type('200.00')
      cy.get('[data-cy="save-payload-btn"]').click()
      
      // Enter reason
      cy.get('[data-cy="change-reason-input"]').type('Corrected amount')
      cy.get('[data-cy="confirm-change-btn"]').click()
      
      cy.wait('@updatePayload')
    })
  })
})
