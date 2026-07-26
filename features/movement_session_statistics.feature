Feature: Trustworthy session statistics

  Context:
  - Problem: the statistics of a finished session are computed with no test coverage at all, from raw
    GPS altitude, and with thresholds expressed in fixed metres. GPS altitude is far noisier than the
    horizontal position, and climb is accumulated by adding every positive difference between
    consecutive points, so a flat outing invents metres it never climbed. The session detail already
    shows that number as "Desnivel", so the app is presenting it as fact today.
  - Savings: a climb figure that can be trusted without checking it against another app, and no need
    to rebuild the history again once the metrics are corrected.
  - Why: out in the hills and on a bike, climb is the number that defines the outing more than the
    kilometres are; the pillar promises visible progress and climb is most of it.

  As a user of Hazlo Sano
  I want the climb, the descent and the time I spent moving to reflect the outing
  So that I can compare one session against another and see real progress

  Note: as with the recorded path, the app never knows whether this was a walk, a jog, a run or a
  bike ride, so no threshold here may be a fixed number of metres either. Readings below the noise
  floor never reach the path, which means a pause shows up as one long gap between two points rather
  than as many short ones: what separates moving from waiting is speed, not distance.

  Scenario: A flat outing does not invent climb
    Given I recorded a session over flat ground
    When the GPS reported altitude wandering up and down around the true one
    Then the climb of the session stays practically at zero
    And so does the descent

  Scenario: A real climb is measured
    Given I recorded a session that went steadily uphill
    When I look at the session statistics
    Then the climb matches the height actually gained

  Scenario: Going up and coming back down
    Given I recorded a session that climbed a hill and returned by the same path
    When I look at the session statistics
    Then the climb and the descent each match the height of the hill
    And they are reported separately

  Scenario: Waiting is not time spent moving
    Given I recorded a session with a long pause in the middle
    When I look at the session statistics
    Then the time spent moving excludes the pause
    And the elapsed time of the session still includes it

  Scenario Outline: Time spent moving is measured at every pace
    Given I recorded a session travelling at <pace> without stopping
    When I look at the session statistics
    Then the time spent moving covers the whole session

    Examples:
      | pace    |
      | walking |
      | jogging |
      | running |
      | cycling |

  Scenario: The highest and lowest points of the session
    Given I recorded a session that crossed a hill
    When I look at the session statistics
    Then the highest and lowest altitudes reflect the terrain rather than the noise

  Scenario: A session with nothing recorded has no statistics
    Given a session that recorded no locations
    When I look at the session statistics
    Then every figure reads as empty instead of as zero progress
