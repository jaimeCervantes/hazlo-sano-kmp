Feature: Capturing a real GPS trace to calibrate the filter

  Context:
  - Problem: the thresholds that decide which GPS readings become part of a session, and how climb is
    accumulated, were calibrated against synthetic noise that is uniform and independent between
    readings. Real GPS error is correlated, and the vertical error especially so. Worse, a recorded
    session only keeps the readings the filter accepted, already smoothed: nothing survives of the
    readings that were thrown away or of why. So an outing can show that the distance was off, but
    never why it was off, and the constants cannot be re-tuned without going out again.
  - Savings: one walk and one bike ride produce a dataset that can be replayed offline. Each round of
    calibration drops from another outing — an hour, plus waiting for the weather — to running a test.
    With five constants to settle, that is the difference between days and an afternoon.
  - Why: the Movement pillar promises visible progress, and progress measured wrong is worse than not
    measured. This is the piece that turns "we trust the design" into "we measured it".

  As someone calibrating Hazlo Sano against a real GPS
  I want every reading the receiver delivered, together with what the filter decided about it
  So that I can replay a real outing offline and settle the thresholds without walking it again

  Note: this is a diagnostic affordance, not a feature of the pillar. It stays off unless deliberately
  turned on, because an app in normal use has no business writing a file for every session. A session
  recorded with it off is not broken — it simply has nothing to diagnose, and says so.

  Scenario: Recording normally leaves no trace behind
    Given the trace capture has not been turned on
    When I record a session
    Then no trace is kept for it
    And the session records the same journey it would have recorded anyway

  Scenario: Every reading is kept, with the verdict the filter gave it
    Given the trace capture is on
    When I record a session
    Then the trace holds every reading the receiver delivered, in the order it arrived
    And each one carries the position, altitude, accuracy and time the receiver reported
    And each one says whether it was accepted into the path or, if not, why it was rejected

  Scenario: The readings thrown away before the session even starts are kept too
    Given the trace capture is on
    And the first readings after starting are too imprecise to use
    When I record a session
    Then those readings appear in the trace as rejected
    And the trace still belongs to the session that was eventually recorded

  Scenario: A trace survives the outing being cut short
    Given the trace capture is on
    And I am recording with the screen off
    When the system kills the app before I stop the recording
    Then the readings collected until that moment are still in the trace

  Scenario: The session detail explains what the filter did
    Given I recorded a session with the trace capture on
    When I open that session and look at its diagnosis
    Then it tells me how many readings arrived and how many became part of the path
    And it breaks the rejected ones down by reason
    And it reports the accuracy the receiver was reporting and how often it actually delivered

  Scenario: A session with no trace says so rather than showing zeros
    Given I recorded a session with the trace capture off
    When I open that session and look for its diagnosis
    Then it tells me there is no trace for this session
    And it does not show counts of zero as if the filter had rejected nothing

  Scenario: The diagnosis stays out of the way
    Given I open a recorded session
    When I have not asked for the diagnosis
    Then the session shows its distance, time, pace and climb as it always did
    And the diagnosis is available without crowding them

  Scenario: A captured trace reproduces the session it came from
    Given a trace captured during a real outing
    When the readings in it are fed through the filter again
    Then the resulting path and distance match the session that was recorded that day
    And changing a threshold changes the result without anyone leaving the desk

  Scenario: A captured trace can be taken off the phone
    Given I recorded a session with the trace capture on
    When I connect the phone to a computer
    Then the trace can be copied off it without special access to the app
