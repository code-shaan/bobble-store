function Order(){
  this._bobbles = {};
  this._order_status = {};
  this._state = undefined;
  this._span = undefined;
}

Order.prototype.reset = function(){
  this._bobbles = {};
  this._order_status = {};
  this._state = undefined;
  this._span = undefined;
}

Order.prototype.addBobble = function(character){
  if(this._bobbles[character] === undefined){
    this._state = "open"
    this._bobbles[character] = 1;
  } else {
    this._bobbles[character]++
  }
}

Order.prototype.items = function(){
  var items = [];
  $.each(this._bobbles,function(character,qty){
    items.push({character:character,quantity:qty});
  })
  return items;
}

Order.prototype.activate = function(){
  this._state = "active";
}

Order.prototype.complete = function(){
  return this.reset();
}

Order.prototype.isOpen = function(){
  return this._state === "open";
}

Order.prototype.isActive = function(){
  return this._state === "active";
}

Order.prototype.isOrdering = function(){
  return this._state === "active" && this._order_status.state === undefined;
}

Order.prototype.isDelivered = function(){
  return this._state === "delivered";
}

Order.prototype.status = function(){
  return this._order_status;
}

Order.prototype.setStatus = function(status){
  this._order_status = status;
  if(status.state === "ready"){
    this._state = "delivered";
  }
}

Order.prototype.orderID = function(){
  return this._order_status.order_id;
}