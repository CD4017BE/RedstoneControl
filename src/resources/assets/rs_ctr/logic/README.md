## Gate Palette Tabs
Each tab in the gate palette of the circuit editor is registered via `cd4017be.rs_ctr.circuit.editor.CircuitInstructionSet.registerTab("<modId>:<tabId>")`.
The file `assets/<modId>/logic/tabs/<tabId>.json` consists of a json list element, containing the `<modId>:<gateId>`s of all gates to appear in that tab.  
The icon image of that tab is `assets/<modId>/textures/gates/<tabId>.png` and its name/description lang key is `gategroup.<modId>.<tabId>`.

## Defining Gates
Each gate is defined in a `assets/<modId>/logic/gates/<gateId>.json` file with the following json structure:

- `"id":` an unique id in range 0...255 used to serialize the gate in schematic files and server-client synchronization (see `usedIDs.txt`).
- `"type": null` the specialized **gate type** to use instead of the default one.
- `"cfg": []` list of **configuration variables** so the user can tweak the behavior of the gate.
- `"width: 3"` width of the gate in grid units from input to output pins (negative width swaps input and output side).
- `"pins_in": ""` a sequence of **link descriptors** defining the arrangement of input pins from top to bottom. Each inserted space character creates a gap of one grid unit between the pins.
- `"pins_out": ""` the output pin arrangement defined in the same way as above. The longer of the two sequences defines the gate height.
- `"link": ""` an optional **link descriptor** used to reference the result of the link gate defined by some gate types (for ex. the Variable Read paired to a Variable Write).
- `"end": ""` an optional **link descriptor** that defines the "side effect result" of this gate that should be always evaluated.
- `"nodes": []` a list of logic nodes that make up the inner workings of the gate.
    - `"typestrict": false` whether the supplied input types must be exactly matching the declared ones in order for this node to apply. Otherwise automatic type conversion is attempted.
    - `"if": {}` an optional condition that must be fulfilled in order for this node to apply.
       - `"var":` index of the **configuration** variable to compare
       - `"eq":` the value it must match
       - `"neq":` the value it must not match (only one of "neq" or "eq" must be defined)
    - `"in": []` list of **link descriptor**s defining the inputs of this node
    - `"sortin": []` list of **link descriptor**s defining additional inputs that may be reordered for optimization.
    - `"out":` **link descriptor** defining the output provided by this node. If the same output is defined by multiple nodes then the first one that has all its requirements met is used.
    - `"code": null` the node's **assembly code** implementation, either directly as list of opcode strings or a single string `"<modId>:<nodeId>"` referencing a `.jasm` file that contains the opcodes. If no code is given at all then the node will just evaluate all inputs and return the only non void value as its output (all inputs must be void in case the output is also void).
    - `"args": []` list of arguments to pass to the **assembly code**: `"$l"` refers to the gate's label, `"$0", "$1",...` refer to **configuration variable** 0, 1, ... (some **gate type**s provide additional parameters on negative indices). Any other string or number is directly passed on as is.

Any elements from the list above that have a default value specified after the colon may be omitted. All other elements however must be present otherwise loading will fail.

The icon image of the gate is `assets/<modId>/textures/gates/<gateId>.png` and its name/description lang key is `gate.<modId>.<gateId>`.  
Gates are automatically loaded when listed in any palette tab.

#### Built-in gate types
- `in` Input Port: parameter `-1` provides the index into the `int[] inputs` field.
- `out` Output Port: parameter `-1` provides the index into the `int[] outputs` field, parameter `-2` provides the update bit to set in `mod`.
- `read` Field read: gate label is used as field name
- `write` Field write: gate label is used as field name
- `readwrite` Field read and write
- `const` Constant: value is parsed from label and provided via parameter `0` (can be passed directly to `ldc`). Does not support additional configuration variables.

Custom gate types are registered in `cd4017be.rs_ctr.circuit.editor.IGateProvider.REGISTRY`.

#### Built-in configuration types
- `value` integer value: typed into text-field
- `interrupt` boolean value: "trigger next cycle" toggle button
- `sign` boolean value: sign toggle button

Custom configurations are registered in `cd4017be.rs_ctr.circuit.data.GateConfiguration.REGISTRY`.

### Link Descriptors
The data flow between the gate's IO pins and the different nodes within are defined via **link descriptors**. They consist of a single (non whitespace) character "variable name" followed by a standard java type descriptor.
Each unique "variable name" defines its own separate data object that may have multiple sinks (output pins, node inputs or side effect result) and one source (input pin, link result or one or more node results).

If the java type differs between source and sink then a conversion is automatically applied if possible:
- All the numeric types `B byte`, `S short`, `C char`, `I int` , `J long` , `F float` and `D double` can convert to one another.
- Any type can convert to `V void` (just ignoring the value) which is a special type that carries no data and is only used to enforce execution orderings (or represent unconnected input pins).
- Any type (except void) can convert to `Z boolean` (by `!= 0` or ` != null`) which can convert back to `I int`.
- Object reference types `L...;` can not automatically convert.

The types `B byte`, `S short`, `C char` are internally handled as `I int` and `Z boolean` is either handled as conditional jump or also as int with `false=0`, `true=-1`.

## Assembly Code
The logic of individual nodes in a gate is implemented in `assets/<modId>/logic/nodes/<nodeId>.jasm` where it is written in Java Virtual Machine Assembly Language with some additional instructions:
- `in <i>` evaluates the **i**-th input (0-indexed) of the node and puts its result on top of the java operand stack.
- `jot <i> <label>` evaluates the **i**-th input of the node as conditional jump on true to **label**
- `jof <i> <label>` evaluates the **i**-th input of the node as conditional jump on false to **label**
- `evdep <i>` tells the compiler that the **i**-th input is only conditionally evaluated by this node so the compiler will ensure its evaluation in other ways in case other nodes need the value as well.
- `evdeps <i>` same as above but also includes the case that this node itself might need the value again after conditionally evaluating it.
- `<type> <var>` reserves a free entry in the local variable table for the given **type** to use in local variable instructions. The variables `this` and `mod` are predefined with the reference to the circuit class instance and `mod` being an integer holding the state modification bit-flags returned by the tick() method: bit0 = trigger next tick, bit_i = output i-1 modified.
- `clr <var>` manually marks the given local variable as free to be reused by other nodes or for storage of intermediate results. However all variables declared within a node will be freed up anyway after the node's code completes.

The file may only contain one instruction or label etc. per line, empty lines and excess whitespace are ignored and everything after a `#` within the same line is treated as comments and therefore also ignored.

The code should implement the node in such way that it effectively puts its result on the top of the java operand stack. In case of void type results, the operand stack remains unchanged. The code basically gets called when another node runs an `in` instruction.
To allow the compiler to properly handle data flow between nodes, a node should make sure the `in` instruction is executed exactly once for each of its inputs. Otherwise if inputs are unused or their `in` instructions are within conditional branches, add the necessary `evdep`s at the beginning. And for inputs that are needed multiple times either duplicate the node inputs in the gate.json file or copy the value using local variables or operand stack dup instructions.

For boolean type results the node may additionally be implemented as conditional jump which can then be called by other nodes via the `jot` and `jof` instructions:  
These implementations are put after the main implementation and start with a `true:` marker line for `jot` and `false:` for the `jof` instruction. Within these blocks the predefined label `dst` represents the passed over jump target argument. These additional implementations are optional and are otherwise automatically generated from the main implementation as needed but manual implementations are sometimes more efficient.

Some instruction arguments can be replaced with `$<i>` to refer to the **i**-th node argument as defined in the gate.json:

- `ldc <val>` accepts number or string arguments for **val**. The instruction is also automatically converted to `sipush`, `bipush` or `iconst_` for small integer values. Numeric expressions are interpreted as integer constant by default, use the prefixes `F`, `D` or `L`to get float, double or long constants instead.
- `getfield <owner> <name> <desc>` can have both **name** and **desc** parameterized as node argument. And **owner** supports the macros `this` for the current class and `super` for the superclass.
- the same for `putfield, getstatic, putstatic, invokestatic, invokespecial, invokevirtual, invokeinterface` (the `invokedynamic` instruction is not available)
- `new <type>` can have **type** parameterized as node argument, the same for `anewarray, checkcast, instanceof`.

