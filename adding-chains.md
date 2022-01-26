# Adding Custom Chains Guide

## 1. Register the item

To register you chain item you need to add it to the `connectiable_chains:chainable` tag.  
This can be done by creating `data/connectiblechains/tags/items/chainable.json` in your `resources` directory.

The contents of the file should look something like this:
```json
{
  "replace": false,
  "values": [
    "examplemod:my_chain_item_id",
    "examplemod:my_other_chain_item_id"
  ]
}
```

## 2. Specify the textures

The textures of your chain are specified in a pseudo model file.
The file has to follow this path convention: 
```
assets/connectiblechains/models/entity/<underscore_sperated_id>.json
```
Where `<underscore_sperated_id>` is the full id of your item where all `:` and `/` have been replaced with `_`.  
I.e.: 
- `examplemod:my_chain_item_id` -> `examplemod_my_chain_item_id`
- `examplemod:chains/my_chain` -> `examplemod_chains_my_chain`

The file has to contain a `textures` object with `chain` and `chain_knot` properties. 
The values specify the texture ids, just like normal model files.

For `minecraft:chain` the filename is `minecraft_chain.json` and the contents are
```json
{
  "textures": {
    "chain": "minecraft:textures/block/chain",
    "chain_knot": "connectiblechains:textures/entity/chain_knot"
  }
}
```

## 3. Create a knot texture

The chain knot (the part around the fence or wall) requires a special texture. 
You can put it wherever you want as long as the model file has the correct `chain_knot` value.
However, we recommend using the `textures/entity` directory.

You can use [src/main/resources/assets/connectiblechains/textures/entity/chain_knot.png](src/main/resources/assets/connectiblechains/textures/entity/chain_knot.png) as a template.

## 4. See if it worked

You should now be able to use the custom chain. 

### If it does not work follow these steps

1. Check the log for errors and fix them
2. If you can't place the chain then you messed up in step 1  
   The tag file **has to** be in `data/connectiblechains/tags/items/`
3. If the texture is magenta and black you messed up in step 2  
   The model file **has to** be in `assets/connectiblechains/models/entity/`