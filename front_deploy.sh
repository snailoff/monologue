#!/bin/bash

npx shadow-cljs release frontend
firebase deploy --project monologue-2da19
