model = Model()
i1 = Input("input", "TENSOR_QUANT8_ASYMM", "{2, 3, 4, 5}, 1.0, 0")
perms = Parameter("perms", "TENSOR_INT32", "{4}", [2, 0, 1, 3])
output = Output("output", "TENSOR_QUANT8_ASYMM", "{4, 2, 3, 5}, 1.0, 0")

model = model.Operation("TRANSPOSE", i1, perms).To(output)

# Example 1. Input in operand 0,
input0 = {i1: # input 0
          [0,   1,   2,   3,   4,   5,   6,   7,   8,   9,   10,  11,
           12,  13,  14,  15,  16,  17,  18,  19,  20,  21,  22,  23,
           24,  25,  26,  27,  28,  29,  30,  31,  32,  33,  34,  35,
           36,  37,  38,  39,  40,  41,  42,  43,  44,  45,  46,  47,
           48,  49,  50,  51,  52,  53,  54,  55,  56,  57,  58,  59,
           60,  61,  62,  63,  64,  65,  66,  67,  68,  69,  70,  71,
           72,  73,  74,  75,  76,  77,  78,  79,  80,  81,  82,  83,
           84,  85,  86,  87,  88,  89,  90,  91,  92,  93,  94,  95,
           96,  97,  98,  99,  100, 101, 102, 103, 104, 105, 106, 107,
           108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119]}

output0 = {output: # output 0
          [0,  1,  2,  3,  4,  20, 21, 22, 23, 24, 40,  41,  42,  43,  44,
           60, 61, 62, 63, 64, 80, 81, 82, 83, 84, 100, 101, 102, 103, 104,
           5,  6,  7,  8,  9,  25, 26, 27, 28, 29, 45,  46,  47,  48,  49,
           65, 66, 67, 68, 69, 85, 86, 87, 88, 89, 105, 106, 107, 108, 109,
           10, 11, 12, 13, 14, 30, 31, 32, 33, 34, 50,  51,  52,  53,  54,
           70, 71, 72, 73, 74, 90, 91, 92, 93, 94, 110, 111, 112, 113, 114,
           15, 16, 17, 18, 19, 35, 36, 37, 38, 39, 55,  56,  57,  58,  59,
           75, 76, 77, 78, 79, 95, 96, 97, 98, 99, 115, 116, 117, 118, 119]}

# Instantiate an example
Example((input0, output0))
