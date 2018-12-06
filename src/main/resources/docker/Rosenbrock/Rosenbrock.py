
import sys
import numpy as np
def rosen(x):
     """""The Rosenbrock function"""""
     return sum(100.0*(x[1:]-x[:-1]**2.0)**2.0 + (1-x[:-1])**2.0)

x = np.array([float(i) for i in sys.argv[1:]])

#f = open('obj.txt', 'w')
#f.write('rosen '+ str(rosen(x)))

print('rosen '+ str(rosen(x)))


