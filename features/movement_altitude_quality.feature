Feature: Knowing whether the altitude can be trusted

  Context:
  - Problem: the first field calibration showed the altitude this phone reports going stale — one
    trace repeated the exact same value for three minutes while its owner cycled 450 metres, and the
    climb he rode during that window is simply not in the data. The app reported 5 m of ascent for a
    steep slope and presented it as fact. Meanwhile Android is asked for none of what it knows about
    that reading: whether it carries an altitude at all, and how accurate it believes the altitude
    to be. The app substitutes a guess of twice the horizontal accuracy for the second, and reads
    the first without checking, which turns a missing altitude into a confident zero metres.
  - Savings: one more outing then decides between two very different paths — discarding altitude the
    system already flags as poor, which is a day's work, or fitting a barometer, which is weeks.
    Choosing without that measurement is choosing blind, and the smoothing constants were already
    once calibrated against synthetic noise that turned out five times more pessimistic than the
    real thing.
  - Why: climb is the figure that defines an outing in the hills more than the kilometres do. Right
    now it is the only one of the eight the app shows that is known to be capable of lying.

  As someone calibrating Hazlo Sano against a real GPS
  I want each reading to carry what the receiver says about its own altitude
  So that the next outing tells us whether bad altitude can be detected or has to be measured another way

  Note: this slice deliberately adds no rule that discards anything. It captures what is currently
  thrown away and stops the app from inventing a vertical accuracy it was never given. What to do
  about a stale altitude is decided from the data this produces, not before it.

  Scenario: A reading carries what the receiver knows about its altitude
    Given the receiver reports an altitude together with how accurate it believes it to be
    When that reading is recorded
    Then the recorded reading carries that accuracy
    And the smoothing uses it instead of a guess derived from the horizontal accuracy

  Scenario: A reading without an altitude says so
    Given the receiver reports a position but no altitude
    When that reading is recorded
    Then the reading carries no altitude rather than an altitude of zero
    And nothing treats it as a position at sea level

  Scenario: A receiver that reports no vertical accuracy still works
    Given the receiver reports an altitude but no accuracy for it
    When that reading is recorded
    Then the altitude is still used
    And the accuracy falls back to being derived from the horizontal one

  Scenario: A session whose readings carried no altitude reports no climb
    Given I recorded a session where the receiver never reported an altitude
    When I open that session
    Then the climb, the descent and the altitudes read as empty
    And nothing claims the outing was flat

  Scenario: The captured trace carries the altitude quality too
    Given the trace capture is on
    When I record a session
    Then each captured reading carries whether it had an altitude and how accurate it was
    And a replayed trace can be judged on that

  Scenario: Traces captured before this still read
    Given a trace captured by an earlier version of the app
    When it is replayed
    Then every reading in it is still read
    And the readings are treated as not saying anything about their vertical accuracy
