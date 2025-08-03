
# 65 -> 90
num_A = ord('A')
num_Z = ord('Z')

# 97 -> 122
num_a = ord('a')
num_z = ord('z')

r = num_z - num_a + 1

diff = num_a - num_A

# range chack to go around from Z->A or z->a would not do sinse the gap between Z and a is pretty short
def int_2_char(n, was_upper):
    if n > num_z:
        n = n - r

    if was_upper:
        n = n - diff
    return chr(n)

def encrypt(_str, token):
    result = ''
    token = token.lower()
    for s in list(_str):
        if s.lower() not in token:
            result = result + s

    return result

def is_upper(n):
    return n >= num_A and n <= num_Z

def shifting_str(s, n):
    num = ord(s.lower())
    was_upper = is_upper(ord(s))
    # print(s, ord(s), num, num + n, chr(num), num + n - r)
    if num >= num_a and num <= num_z:
        return int_2_char(num + n, was_upper)
    
    return s

# Assume n >= 0
def caesar(_str, n, token):
    shifted_str = ''.join([shifting_str(s, n) for s in list(_str)])
    # print(shifted_str)
    return encrypt(shifted_str, token)


TOKEN = 'ju51abeir73'

TEST_CASES = [
    {
        'input': {
            'str': "Hello",
            'n': 4
        },
        'expected': "Lpps"
    },
    {
        'input': {
            'str': "abc",
            'n': 0
        },
        'expected': "c"
    },
    {
        'input': {
            'str': "Zebra",
            'n': 1
        },
        'expected': "fcs"
    },
    {
        'input': {
            'str': "xyzXYZ",
            'n': 3
        },
        'expected': "cC"
    },
    {
        'input': {
            'str': "Hello, World!",
            'n': 13
        },
        'expected': "yy, yq!"
    },
    {
        'input': {
            'str': "",
            'n': 5
        },
        'expected': ""
    },
    {
        'input': {
            'str': "aaaa",
            'n': 0
        },
        'expected': ""
    }
]

for case in TEST_CASES:
    result = False
    _input = case['input']
    _str = _input['str']
    n = _input['n']

    expected = case['expected']
    try:
        actual = caesar(_str, n, TOKEN)

        result = expected == actual

        print(_input, ' -> ', expected, ' == ', actual, ' = ', result)
    except Exception as e:
        print(_input, ' -> ', 'Error: ', e)