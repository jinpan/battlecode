DEFAULT_AI = 'basicplayer'

class STATUS:
    OPEN = '1'
    PENDING = '2'
    CLOSED = '3'

class RESULT:
    ROBOT_A = 'A'
    ROBOT_B = 'B'
    TIE = 'T'

STATUS_CHOICES = (
    (STATUS.OPEN, 'OPEN'),
    (STATUS.PENDING, 'PENDING'),
    (STATUS.CLOSED, 'CLOSED'), 
)

RESULT_CHOICES = (
    (RESULT.ROBOT_A, 'ROBOT_A'),
    (RESULT.ROBOT_B, 'ROBOT_B'),
    (RESULT.TIE, 'TIE')
)


sleep_interval = 0.25

