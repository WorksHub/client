var physicsjs = function(){};

/** @interface */
var Body = function(){};
Body.prototype.x;
Body.prototype.y;
Body.prototype.vx;
Body.prototype.vy;
Body.prototype.radius;
Body.prototype.name;
Body.prototype.mass;
Body.prototype.restitution;
Body.prototype.cof;
Body.prototype.state;
Body.prototype.state.pos;
Body.prototype.state.vel;
Body.prototype.state.acc;
Body.prototype.state.angular;
Body.prototype.state.angular.pos;
Body.prototype.state.angular.vel;
Body.prototype.state.angular.acc;

/** @return {!Body} */
physicsjs.body = function(name, params){};

physicsjs.aabb = function(minx, miny, maxx, maxy){};

/** @interface */
var Behavior = function(){};
Behavior.prototype.pos;
Behavior.prototype.strength;
Behavior.prototype.order;

/** @return {!Behavior} */
physicsjs.behavior = function(name, params){};

/** @interface */
var Scratch = function() {};
Scratch.prototype.vector = function() {};
Scratch.prototype.transform = function() {};
Scratch.prototype.event = function() {};

/** @return {!Scratch} */
physicsjs.scratchpad = function() {};
