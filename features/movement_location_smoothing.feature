Feature: Trustworthy recorded distance

  Context:
  - Problem: every location the phone reports is accumulated as-is. GPS readings drift by several
    metres even when the phone is still, so waiting at a traffic light, sitting under trees or
    walking between buildings keeps adding metres that nobody walked. The same noisy points feed
    ascent, pace and moving time, so one bad signal spreads across the whole session.
  - Savings: distances that can be trusted without repeating the outing or correcting them by hand,
    and no need to rebuild the history later with different numbers.
  - Why: the Movement pillar promises visible progress. Inflated kilometres make that progress
    fiction, and routes, guided navigation and statistics are all built on these very same points.

  As a user of Hazlo Sano
  I want the distance of my session to reflect what I actually travelled
  So that I can trust what the app tells me without checking it against another app

  Note: the app never knows which activity is being recorded — there is no activity selector and a
  session carries no activity type. At the interval locations are sampled at, a walk advances under
  three metres between readings while a bike ride advances tens of them, with jogging and running in
  between. The filter therefore has to adapt to the pace it observes instead of being tuned for one
  of them, and no threshold may be expressed as a fixed number of metres.

  Scenario: Standing still does not add distance
    Given I am recording a session
    When I stay in the same place while the GPS keeps reporting slightly different positions
    Then the recorded distance stays practically at zero

  Scenario Outline: The distance is true at every pace the app is used at
    Given I am recording a session
    When I travel at <pace>
    Then the recorded distance matches the distance travelled
    And none of those readings is treated as an impossible jump

    Examples:
      | pace                  |
      | walking               |
      | jogging               |
      | running               |
      | cycling               |
      | cycling downhill fast |

  Scenario Outline: A poor signal costs precision, not the journey
    Given I am recording a session where the GPS only manages coarse fixes
    When I travel at <pace>
    Then the recorded distance still reflects the journey rather than the noise

    Examples:
      | pace    |
      | walking |
      | jogging |
      | running |
      | cycling |

  Scenario: Changing pace within one session
    Given I am recording a session
    When I alternate between travelling and standing still
    Then the moving stretches keep their distance
    And the pauses do not add distance

  Scenario: A single wild reading does not corrupt the session
    Given I am recording a session with a clean signal
    When one location implies a speed no rider could reach in the time elapsed
    Then that jump is not counted as travelled distance
    And the recording continues from where I actually am

  Scenario: Poor quality readings are not recorded
    Given I am recording a session
    When a location arrives with an accuracy too poor to tell movement from noise
    Then it is neither added to the path nor counted as distance

  Scenario: The first location starts the path
    Given I just started recording
    When the first location arrives
    Then it becomes the starting point of the path without adding distance

  Scenario: Each recording starts from a clean slate
    Given I finished recording a session
    When I start a new one
    Then the new session's distance does not carry anything over from the previous one
