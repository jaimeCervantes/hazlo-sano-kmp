Feature: Movement session detail

  Context:
  - Problem: The history lists a session's date, distance and time, but the traveled route — the
    most valuable part of a recording and already stored point by point — is never shown. The user
    cannot see where they went, only that they went somewhere.
  - Savings: Reuses the points already persisted (getSessionPoints) and the existing map surface,
    so no new storage or platform work is needed to close the record -> save -> review loop.
  - Why: The Movement pillar is about seeing progress; the route is what makes a past session
    recognizable and worth keeping, and it is the base for later comparisons between sessions.

  As a user of Hazlo Sano
  I want to open a recorded session and see its route on the map
  So that I can recognize where I went and review how that session went

  Scenario: Opening the detail of a recorded session
    Given the session history lists a recorded session
    When I tap that session
    Then the session detail becomes visible with the session name and date

  Scenario: Seeing the traveled route of the session
    Given a session was saved with a traveled path
    When I open its detail
    Then the map shows the saved path
    And the map view is framed to the bounds of that path

  Scenario: Seeing the summary of the session
    Given a session was saved with distance, time and elevation gain
    When I open its detail
    Then I see its distance, elapsed time, average pace and elevation gain

  Scenario: Opening a session that has no stored points
    Given a session was saved without a traveled path
    When I open its detail
    Then I see its summary and a message telling me the route was not stored

  Scenario: Returning from the detail to the history
    Given a session detail is open
    When I tap the back control
    Then the session history is visible again
