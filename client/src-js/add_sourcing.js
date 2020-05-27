export function addSourcing(obj, atx) {
  var sourcing = {};

  if (atx.referrer != null && atx.referrer != '') {
    sourcing.referrer = atx.referrer;
  }

  if (atx.utms && atx.utms.size > 0) {
    const utmsEntries = Array.from(atx.utms.entries());

    sourcing.campaign = Object.assign(
      {}, ...utmsEntries.map(([k, v]) => ({ [k]: v })));

    // also add without the `utm_` part
    sourcing.campaign = Object.assign(
      sourcing.campaign,
      ...utmsEntries.map(([k, v]) => ({ [k.replace('utm_', '')]: v }))
    );
  }

  if (Object.keys(sourcing).length > 0) {
    obj.sourcing = sourcing;
  }

  return obj;
}
