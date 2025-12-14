// ***********************************************************
// This support file is loaded before each E2E spec file.
// You can define global behaviors and custom commands here.
// ***********************************************************

// Custom command to intercept API calls with mock data
Cypress.Commands.add('mockApi', () => {
  cy.intercept('GET', '/api/dashboard/overview', { fixture: 'dashboard.json' }).as('getDashboard')
  cy.intercept('GET', '/api/workflows*', { fixture: 'executions.json' }).as('getExecutions')
  cy.intercept('GET', '/api/workflows/stats', { fixture: 'stats.json' }).as('getStats')
  cy.intercept('GET', '/api/workflows/recent', { fixture: 'recent.json' }).as('getRecent')
  cy.intercept('GET', '/api/metrics', { fixture: 'metrics.json' }).as('getMetrics')
  cy.intercept('GET', '/api/circuit-breakers', { fixture: 'circuitBreakers.json' }).as('getCircuitBreakers')
})

// Custom command to get execution by id
Cypress.Commands.add('mockExecution', (executionId) => {
  cy.intercept('GET', `/api/workflows/${executionId}`, { fixture: 'execution.json' }).as('getExecution')
})

// Custom command to mock resume action
Cypress.Commands.add('mockResume', (executionId) => {
  cy.intercept('POST', `/api/workflows/${executionId}/resume*`, { statusCode: 202 }).as('resumeExecution')
})

// Custom command to mock cancel action
Cypress.Commands.add('mockCancel', (executionId) => {
  cy.intercept('DELETE', `/api/workflows/${executionId}`, { statusCode: 204 }).as('cancelExecution')
})

// Log API responses for debugging
Cypress.on('uncaught:exception', (err, runnable) => {
  // Prevent Cypress from failing on uncaught exceptions from the app
  console.error('Uncaught exception:', err.message)
  return false
})
