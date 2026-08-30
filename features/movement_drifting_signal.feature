Feature: A phone that is not moving records no distance

  Context:
  - Problem: the filter decides whether a reading is movement by comparing it against the accuracy
    that same reading declares about itself. Measured in the field on 2026-08-29: after a short bike
    ride the phone sat indoors for 31 minutes with the recording still running, and the receiver kept
    delivering fixes that claimed +/-6 m while landing a median of 31 m from the previous one every
    six seconds. The filter accepted them one after another. The app would have recorded 2832 m for
    an outing whose real part measures about 480 m of raw track, with 84% of the accepted path
    accumulated after the bike had stopped, and 391 s of "moving time" for a phone on a table. The
    synthetic noise the filter was calibrated against never behaves like this: it is small, symmetric
    and honest about its own accuracy, so "standing still adds no distance" passes in the suite and
    fails on the street.
  - Savings: a session that does not have to be deleted or corrected by hand, and a history whose
    total distance means something. Today one forgotten recording injects kilometres that nobody can
    tell apart from the real ones afterwards, and the only defence is remembering to press stop.
  - Why: distance is what the Movement pillar promises. A number inflated fivefold is worse than no
    number, because it gets believed — the same way the truncated moving time was believed until it
    was measured.

  As a user of Hazlo Sano
  I want a session to record what I travelled and not what the receiver imagined
  So that a forgotten recording, a coffee stop or a roof over my head does not invent kilometres

  Note: the discriminating evidence is not the accuracy a reading claims — the field trace shows that
  number to be optimistic exactly when it matters, and a filter that only reads it has nothing left
  to defend itself with. The evidence is persistence: real movement keeps going somewhere, noise
  leaves and comes back. Over one-minute windows the parked phone displaced a median of 6 m from
  where it had been while its path added up to hundreds of metres; the same windows measured 113 m
  walking and 137 m cycling. That gap is what the filter has to read.

  Note: as everywhere else in this pillar, no threshold may be a fixed number of metres — the app
  never knows whether it is recording a walk or a bike ride. Thresholds are expressed against the
  reading's own accuracy and against the time actually elapsed.

  @slice-1
  Scenario: A phone that does not move records nothing, however far the signal wanders
    Given I am recording a session
    When the receiver reports positions tens of metres apart while I stay where I am
    And it claims those fixes are precise
    Then the recorded distance stays practically at zero
    And the path does not grow

  @slice-1
  Scenario: An excursion that comes back is not a journey
    Given I am recording a session
    When one reading lands hundreds of metres away and the following ones are back where I was
    Then none of that excursion is added to the path
    And the recording continues from where I actually am

  @slice-1
  Scenario: A fix that arrives somewhere nobody could have reached is not travel
    Given I am recording a session and I have not been going anywhere
    When the receiver puts me 168 m away nine seconds later and holds itself there
    Then those readings are not counted as travel
    And what makes them impossible is what I have been doing lately, not a fixed speed limit
    And a real start from a standstill is not caught by the same rule

  @slice-1
  Scenario: Movement is counted in full once it is confirmed
    Given I am recording a session and I have been standing still
    When I start travelling away from where I stood
    Then the metres I travel are recorded, including the first ones
    And confirming the movement delays the distance rather than losing it

  @slice-1
  Scenario Outline: A real journey keeps its distance at every pace the app is used at
    Given I am recording a session
    When I travel at <pace>
    Then the recorded distance still matches the distance travelled

    Examples:
      | pace                  |
      | walking               |
      | jogging               |
      | running               |
      | cycling               |
      | cycling downhill fast |

  @slice-1
  Scenario: A pause in the middle of an outing costs nothing but the pause
    Given I am recording a session
    When I travel, stop for a while where the signal keeps wandering, and travel again
    Then the pause adds no distance
    And both travelled stretches keep theirs

  @slice-1
  Scenario: Time spent not moving is not reported as moving time
    Given I recorded a session where I stopped moving long before I stopped recording
    When I open that session
    Then the moving time covers the part I travelled and not the part I sat through

  @slice-1
  Scenario: The diagnosis says the readings were discarded for wandering
    Given I recorded a session with the trace capture on where the receiver wandered
    When I look at its diagnosis
    Then those readings are counted under a reason of their own
    And it is distinct from a reading discarded for being within the noise of a still signal

  @slice-1
  Scenario: The signal used in tests wanders the way the measured one did
    Given a synthetic trace built for a phone that is not moving
    When it is generated
    Then its readings excurse as far as the field trace measured, and claim to be precise while doing it
    And the previous synthetic noise no longer stands in for a signal that behaves nothing like it

  @slice-2
  Scenario Outline: The recording says what it is waiting for rather than showing a zero
    Given a recording <state>
    When I look at the distance
    Then it reads <shown>

    Examples:
      | state                                          | shown                    |
      | that has not had a fix yet                     | that it is looking       |
      | whose path is only the point it started from   | that it is confirming    |
      | that has got somewhere                         | the distance travelled   |
      | that is not running                            | the zero it always shows |

  @slice-2
  Scenario: A recording that has gone nowhere for a long time says so
    Given I am recording a session
    When the receiver has kept reporting for five minutes without the path growing
    Then the tracker tells me how long I have not been moving
    And the notification says it too, because that is where a forgotten recording is visible

  @slice-2
  Scenario: Losing the signal is not the same as not moving
    Given I am recording a session
    When the receiver stops reporting altogether
    Then the app does not tell me I have stopped moving
    And the time without a signal does not age into a claim about me

  @slice-2
  Scenario: Saving a session that ended long before it was stopped
    Given a recording that kept reporting from the same place for half an hour
    When I finally press stop
    Then the session that is saved ends where it last moved
    And what was recorded is still all there

  @slice-2
  Scenario: A session that lost its signal keeps its whole duration
    Given a recording whose receiver went quiet before I pressed stop
    When I press stop
    Then the session keeps every second of it
    And nothing is trimmed on evidence the app never had
