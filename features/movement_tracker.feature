Feature: Movement tracker screen

  Context:
  - Problem: Tapping the "Movimiento" pillar card opens nothing; the tracker is half-built
    (domain + map helpers only) with no UI or navigation wiring.
  - Savings: Unlocks the Movement pillar by reusing the existing domain and MapLibre helpers,
    avoiding rework.
  - Why: Movement is one of the four core pillars; without it the "Cultiva tus 4 pilares"
    promise is incomplete.

  As a user of Hazlo Sano
  I want to open the movement tracker from the home screen
  So that I can see the map and start engaging with the Movement pillar

  Scenario: Opening the tracker from the movement pillar card
    Given I am on the home screen
    And the movement pillar card has a tracker action
    When I tap the movement pillar card
    Then the tracker screen becomes visible

  Scenario: Returning from the tracker to the home screen
    Given the tracker screen is open
    When I tap the back control
    Then the tracker screen is closed and the home screen is visible again
