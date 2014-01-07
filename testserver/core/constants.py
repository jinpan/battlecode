DEFAULT_AI = 'examplefuncsplayer'

class STATUS:
    OPEN = '1'
    PENDING = '2'
    CLOSED = '3'
    FAILED = '4'

class RESULT:
    ROBOT_A = 'A'
    ROBOT_B = 'B'
    TIE = 'T'

STATUS_CHOICES = (
    (STATUS.OPEN, 'OPEN'),
    (STATUS.PENDING, 'PENDING'),
    (STATUS.CLOSED, 'CLOSED'), 
    (STATUS.FAILED, 'FAILED'), 
)

RESULT_CHOICES = (
    (RESULT.ROBOT_A, 'ROBOT_A'),
    (RESULT.ROBOT_B, 'ROBOT_B'),
    (RESULT.TIE, 'TIE')
)


DEFAULT_SLEEP_INTERVAL = 0.25
NUM_MAPS = 53

