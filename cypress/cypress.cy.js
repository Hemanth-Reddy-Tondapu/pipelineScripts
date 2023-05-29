describe('Complex Test Case', () => {
  it('should perform a series of actions and assertions', () => {
    cy.visit('/'); // Visit the homepage of the application

    // Log in to the application
    cy.get('#username').type('myusername');
    cy.get('#password').type('mypassword');
    cy.get('#login-button').click();

    // Assert that the user is logged in
    cy.get('.welcome-message').should('contain', 'Welcome, myusername');

    // Navigate to a product page
    cy.get('.menu').contains('Products').click();
    cy.url().should('include', '/products');

    // Filter and sort products
    cy.get('#filter-input').type('Cypress');
    cy.get('#filter-button').click();
    cy.get('#sort-dropdown').select('Price: High to Low');

    // Verify the filtered and sorted products
    cy.get('.product-item')
      .should('have.length', 3) // Assuming 3 products are displayed
      .each((product, index) => {
        cy.wrap(product).contains('Cypress Product');
        if (index > 0) {
          const currentPrice = parseFloat(product.find('.price').text().substring(1));
          const previousPrice = parseFloat(cy.wrap(product).prev().find('.price').text().substring(1));
          expect(currentPrice).to.be.lessThan(previousPrice);
        }
      });

    // Add a product to the cart
    cy.get('.product-item').first().find('.add-to-cart-button').click();
    cy.get('.cart-items-count').should('have.text', '1');

    // Proceed to checkout
    cy.get('.cart-items-count').click();
    cy.get('.checkout-button').click();

    // Fill out and submit the checkout form
    cy.get('#shipping-name').type('John Doe');
    cy.get('#shipping-address').type('123 Main St');
    cy.get('#shipping-city').type('New York');
    cy.get('#shipping-zip').type('12345');
    cy.get('#payment-method').select('Credit Card');
    cy.get('#credit-card-number').type('1234567890123456');
    cy.get('#credit-card-expiry').type('12/24');
    cy.get('#credit-card-cvv').type('123');
    cy.get('#place-order-button').click();

    // Assert the order confirmation message
    cy.get('.order-confirmation-message').should('contain', 'Your order has been placed successfully.');

    // Log out
    cy.get('.logout-button').click();
    cy.get('.login-form').should('be.visible');
  });
});

