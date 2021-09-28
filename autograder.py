import csv
import os
import sys
import re

# Comment mein
# 
# #concurrency lmao
# 
# 
# Ok so the idea is that you first take files from command line, and from the first line,
# figure out if they're Fugitive, and in what game.
# 
# Then you have a dictionary of games, and in each game in this dict, verify that there's exactly one 
# Fugitive
# 
# Then, you can read the files line by line
# 
#  view[i][j][k] is the square ID i thinks ID k is as per feedback of Move j

# dict of game, view

"""
For each game, associate the files which are traces of clients playing that game

There should be exactly one file that has the Fugitive trace.

IN the end for each game, 
 verify that view is consistent, i.e. for all i, i', for all j, k
 view[i][j][k] == view[i'][j][k], or if not, either of the views is undefined

 if view[i][j][k] is defined for some k, it is defined for all k, except possibly Fugitive
"""

games = {}

def compare_views(view1, view2):
    out = [i==j or i==-2 or j==-2 for i,j in zip(view1, view2)]
    return sum(out) == len(view1)

def logerr(*args):
    print(*args)
    exit(1)

def straight(start, fin):
    return (start%8 == fin%8) or (start//8 == fin//8)

def diagonal(start, fin):
    sr = start//8
    fr = fin//8
    sc = start%8
    fc = fin%8
    return (sr + sc == fr + fc) or (sr - sc == fr - fc)

def valid_move(start, fin, id):
    if (id == -1):
        return straight(start, fin) or diagonal(start, fin)
    else:
        return straight(start, fin)

def prop_view(game, id, move, view):
    s = set(games[game]['views'][id][move])
    check = len(s) == 1 and -2 in s
    if not check:
        logerr("Conflict on Id", id, "game", game, "move", move)
    
    games[game]['views'][id][move] = view
    return
    
def autograde(files):

    for file in files:
        print("Reading file: ", file)
        with open(file, 'r', newline = '') as csvfile:
            header = csvfile.readline()
            h = header.split(" ")

            if len(h) < 3: continue

            is_fugitive = h[3] == 'Fugitive'
            id = -1 if is_fugitive else int(h[4])
            game = h[7] if not is_fugitive else h[6]
            start = 42 if is_fugitive else 0
            
            last_move = None
            last_sq = start

            if not game in games.keys(): 
                games[game] = {
                    'fugitive_count': 0,
                    'views': [[[-2 for _ in range(6)] for _ in range(26)] for _ in range(6)],
                    'last': [(0, None) for _ in range(6)]
                }
            
            games[game]['fugitive_count'] += int(is_fugitive)

            for row in csv.reader(csvfile, delimiter = ';'):
                move = int(row[0].split(" ")[-1])
                state = row[2].strip()
                view = [int(i) for i in re.split(" |, ", row[3])[3:]]

                games[game]['last'][id] = (move, games[game]['last'][id][1])

                if state == 'Victory' or state == 'Defeat':
                    if games[game]['last'][id][1] is not None:
                        logerr("Game terminated incorrectly")
                    games[game]['last'][id] = (games[game]['last'][id][0], state == 'Victory')


                view.append(-2)
                if len(row) == 5:
                    view[-1] = int(row[4].split(' ')[-1])

                square = view[id]
                if (not valid_move(last_sq, square, id)):
                    print(last_sq, square, id)
                    logerr("not valid move", game, move, id)
                last_sq = square

                if is_fugitive: # is fugitive
                    if last_move is None:
                        if move != 0 and move != 1:
                            logerr("fugitive not starting correctly")
                        elif move == 0:
                            s=set(view[:-1])
                            check = len(s) == 1 and -1 in s
                            if not check: logerr("Timestep not incremented correctly", move)

                        elif move == 1:
                            prop_view(game, id, move, view)
                    else:
                        if move == last_move:
                            s=set(view[:-1])
                            check = len(s) == 1 and -1 in s
                            if not check: logerr("Timestep not incremented correctly", move)

                        elif move != last_move + 1:
                            logerr("Timestep not incremented correctly")
                        else:
                            prop_view(game, id, move, view) 
                else: # is detective
                    if last_move is None:
                        if move <= 0:
                            logerr("Timestep not incremented correctly", move)

                        prop_view(game, id, move, view)
                    else:
                        if last_move + 1 != move:
                            logerr("Detective not in sync, Move", move, id)
                        else:
                            prop_view(game, id, move, view)
                
                last_move = move
    
    for game in games:

        game = games[game]

        if game['fugitive_count'] != 1:
            logerr("More than 1 fugitives")

        fugitive_last_move = game['last'][-1][0]
        if not game['last'][-1][1] is None: 
            fugitive_result = game['last'][-1][1]
            if fugitive_last_move != max([i[0] for i in game['last']]):
                logerr("Game not terminated correctly")
            for i in range(5):
                if game['last'][i][0] == fugitive_last_move and game['last'][i][1] != (not fugitive_result):
                    logerr("Game not terminated correctly")
        else:
            if max([i[0] for i in game['last']]) > fugitive_last_move + 1:
                logerr("Game not terminated correctly")
            for i in range(5):
                if game['last'][i][0] == fugitive_last_move + 1 and game['last'][i][1] != True:
                    logerr("Game not terminated correctly")

        for move in range(26):
            for i in range(6):
                for j in range(6):
                    if not compare_views(game['views'][i][move], game['views'][j][move]):
                        print("Inconsistent view, Move ", move)

if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    
    parser.add_argument('-t', type = str, nargs = "+")

    args = parser.parse_args()

    autograde(args.t)