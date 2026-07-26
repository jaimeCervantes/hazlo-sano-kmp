Feature: Session figures derived from what was recorded

  Context:
  - Problem: every figure the app reports about a session — distance, climb, descent, time spent
    moving, highest and lowest point — is a pure function of the points that session stored. They are
    nonetheless written into columns when the recording stops, which makes them a cache frozen at
    whatever version of the algorithm wrote them. Improving how the app measures therefore leaves
    every session already recorded reporting the old, wrong number, forever. On top of that, half of
    those figures never reach the screen at all, and a session that measured nothing shows zeros,
    which claims a flat outing at sea level rather than admitting there is no reading.
  - Savings: a calibration round stops invalidating the outings already recorded — retune a threshold
    and every past session reflects it, so one outing can be compared against another. And a new
    metric stops costing a schema change: gradient, rate of climb or time per altitude band become a
    function anyone can add, working over the whole history.
  - Why: the pillar promises visible progress. Progress you cannot compare between outings is not
    visible, and a figure that was computed and never shown does not exist.

  As a user of Hazlo Sano
  I want a session to report what its recorded route actually shows
  So that improving the app improves what it tells me about the outings I already made

  Note: what the session stores does not change — the full list of readings the filter accepted is
  already kept for every session. What changes is that the figures come from those readings when the
  session is opened, instead of from numbers written once when it stopped.

  Note: the elapsed time and the date stay recorded rather than derived. Elapsed time is wall clock
  from start to stop and includes the stretches before the first fix and after the last, so it cannot
  be recovered from the route.

  Scenario: Improving how the app measures also improves what I already recorded
    Given I recorded several outings
    When the app is improved to measure distance or climb more truthfully
    And I open one of those outings again
    Then its figures reflect the better measurement
    And I did not have to record it again

  Scenario: The detail reports everything the route shows
    Given I recorded a session over varied terrain
    When I open that session
    Then it shows the distance, the time it took and the time I spent moving
    And it shows the climb and the descent as two separate figures
    And it shows the highest and the lowest altitude of the route

  Scenario: Waiting is not time spent moving
    Given I recorded a session with a long pause in the middle
    When I open that session
    Then the time spent moving excludes the pause
    And the time the outing took still includes it

  Scenario: A session that recorded no altitude admits it
    Given I recorded a session whose readings carried no altitude
    When I open that session
    Then the altitudes, the climb and the descent read as empty
    And nothing claims the outing was flat at zero metres

  Scenario: A session with nothing recorded has nothing to report
    Given a session that recorded no locations
    When I open that session
    Then every figure reads as empty instead of as zero progress

  Scenario: A finished session reports no live figures
    Given I recorded a session that ended at the top of a climb
    When I open that session
    Then it does not report a current pace or a current gradient
    And nothing describes the last few seconds before I pressed stop as if it described the outing

  Scenario: The history and the detail agree
    Given I recorded an outing
    And the app has since been improved to measure it more truthfully
    When I see that outing in the history and then open it
    Then both show the same distance

  Scenario: The outings I already recorded survive the change
    Given sessions recorded before the figures were derived
    When I open the history after updating the app
    Then those sessions are still listed with their date, their name and their route
    And opening one shows its figures measured from the route it stored

  Scenario: The added figures do not crowd the ones already there
    Given I open a recorded session
    When I look at its summary
    Then distance, time, pace and climb are still readable at a glance
    And the rest of the figures are laid out without pushing the map off the screen
