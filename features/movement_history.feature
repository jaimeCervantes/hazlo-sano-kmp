Feature: Movement session history

  Context:
  - Problem: Recorded sessions are saved in the database but never shown. After "Detener" the
    session disappears from the user's view, so recording has no payoff and persistence can only
    be checked by inspecting the database.
  - Savings: Turns already-built storage into visible progress with no new infrastructure (the
    read side of MovementSessionRepository already exists), and removes the need to use adb to
    verify that a recording was saved.
  - Why: The Movement pillar is about seeing progress over time; a list of sessions is the
    smallest unit of progress and the base for later streaks and goals across the four pillars.

  As a user of Hazlo Sano
  I want to see the sessions I have recorded
  So that I can confirm my activity and follow my progress

  Scenario: Opening the history from the tracker
    Given the tracker screen is open
    When I tap the history action
    Then the session history becomes visible

  Scenario: Listing a recorded session
    Given a session was recorded and saved with a traveled path
    When I open the session history
    Then I see a row for that session with its date, distance and elapsed time

  Scenario: Ordering sessions with the most recent first
    Given two sessions were saved on different dates
    When I open the session history
    Then the most recent session is listed first

  Scenario: Showing an empty history
    Given no session has been saved
    When I open the session history
    Then I see a message inviting me to record my first session

  Scenario: Returning from the history to the tracker
    Given the session history is open
    When I tap the back control
    Then the tracker screen is visible again
