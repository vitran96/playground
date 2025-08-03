def extract_1st_last_digit(n):
    digits = list(str(n))
    return digits[0], digits[-1]

def first_and_last_digit_pair_sum(arr):
    # save max num
    # if a pair, sum and save the max
    highest = 0
    i = 0
    size = len(arr)
    while i < size - 1:
        n = arr[i]
        i_first_digit, i_last_digit = extract_1st_last_digit(n)
        j = i + 1
        while j < size:
            # can cache extracted digit
            m = arr[j]
            # print(n, m, highest)
            j_first_digit, j_last_digit = extract_1st_last_digit(m)

            # print(i_first_digit, i_last_digit, j_first_digit, j_last_digit)
            if i_first_digit == j_first_digit and i_last_digit == j_last_digit:
                highest = max(highest, n + m)
            j = j + 1
        i = i + 1
    return highest


TEST_CASES = [
    {
        'input': [],
        'expected': 0
    },
    {
        'input': [120, 23, 13, 33, 92, 10, 100, 103],
        'expected': 220
    },
    {
        'input': [11, 22, 33, 44],
        'expected': 0
    },
    {
        'input': [121, 131, 141, 151, 161],
        'expected': 312
    },
    {
        'input': [101, 210, 301, 401, 501],
        'expected': 0
    }
]


for case in TEST_CASES:
    result = False
    _input = case['input']

    expected = case['expected']
    try:
        actual = first_and_last_digit_pair_sum(_input)

        result = expected == actual

        print(_input, ' -> ', expected, ' == ', actual, ' = ', result)
    except Exception as e:
        print(_input, ' -> ', 'Error: ', e)
