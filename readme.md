# interface

Utilities for exploring Java models.

`Configuration` is an annotation to describe the model.

`Walk` contains methods for walking annotated Java models.

`Events` contains methods for parsing the models with pidgoon grammars and streams of `InterfaceEvent`.

`InterfacePath` is a class for describing a location in a document.  Use it to keep track of a transversal path and report the location of errors.