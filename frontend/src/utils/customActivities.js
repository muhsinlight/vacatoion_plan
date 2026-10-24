export function addCustomActivityName(list, name) {
  const trimmed = name?.trim()
  if (!trimmed || list.includes(trimmed)) {
    return list
  }
  return [...list, trimmed]
}

export function appendCustomActivity(itinerary, name) {
  const trimmed = name?.trim()
  if (!trimmed || !Array.isArray(itinerary) || !itinerary.length) {
    return itinerary
  }
  const days = itinerary.map((day) => ({
    ...day,
    items: [...(day.items || [])],
  }))
  const already = days.some((day) =>
    day.items.some((item) => item.type === 'ACTIVITY' && item.title === trimmed),
  )
  if (already) {
    return itinerary
  }
  const open = days.find((day) => !day.items.some((item) => item.type === 'ACTIVITY'))
  const target = open || days[days.length - 1]
  const activity = { type: 'ACTIVITY', title: trimmed, estimatedMinutes: 0, note: null }
  const lunch = target.items.findIndex((item) => item.title === 'Öğle yemeği')
  if (lunch === -1) {
    target.items.push(activity)
  } else {
    target.items.splice(lunch, 0, activity)
  }
  return days
}

export function removeCustomActivityName(list, name) {
  return list.filter((item) => item !== name)
}

export function toggleInterest(list, id) {
  const set = new Set(list)
  if (set.has(id)) {
    set.delete(id)
  } else {
    set.add(id)
  }
  return [...set]
}

export function toggleActivityId(list, id) {
  const set = new Set(list)
  if (set.has(id)) {
    set.delete(id)
  } else {
    set.add(id)
  }
  return [...set]
}
