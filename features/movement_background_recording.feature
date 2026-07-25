Feature: Background movement recording

  Context:
  - Problem: recording lives inside the tracker screen's ViewModel, so it only survives while the app
    is in the foreground with the screen on. Pocketing the phone, answering a message or letting the
    screen time out stops the location updates, and the saved session ends up short or empty.
  - Savings: avoids losing whole outings and having to repeat them, and removes the need to check
    every recording afterwards to find out whether it is usable.
  - Why: the Movement pillar promises progress over time; history, detail and statistics are all
    built on recorded sessions, so a recording that cannot be trusted invalidates the whole pillar.

  As a user of Hazlo Sano
  I want my session to keep recording while the app is in the background or the screen is off
  So that the whole outing is captured without keeping the phone awake in my hand

  Scenario: Recording continues while the app is not in the foreground
    Given I started recording a session
    When I leave the tracker screen and keep moving
    Then the traveled path, distance and elapsed time keep growing

  Scenario: An ongoing recording is visible outside the app
    Given I started recording a session
    Then a persistent notification shows the recorded distance and elapsed time
    And the notification updates as the session progresses

  Scenario: Returning to the tracker shows the session in progress
    Given a recording is in progress while the app is in the background
    When I open the tracker screen again
    Then it shows the session as recording, with the distance, time and path recorded so far

  Scenario: Stopping the recording saves the session and clears the notification
    Given a recording is in progress
    When I tap the stop control
    Then the session is saved with everything recorded, including what was recorded in the background
    And the persistent notification disappears

  Scenario: Recording without permission to post notifications
    Given the notification permission is denied
    When I start recording a session
    Then the session is still recorded
