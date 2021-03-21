import { setQueryParam, deleteQueryParam, addQueryParam, removeQueryParam } from './util';

// Our code, currently, relies on whTags being public.
// TODO: Investigate and fix properly without binding it to window
window.whTags = null;
const whTagGroupLimit = 16;

function resetTagsElementVisibility(tagBox) {
    let tagsElements = tagBox.querySelectorAll('.tags--top-level');

    tagsElements.forEach(tagElement => {
        const hasSelectedTags =
            tagElement.getElementsByClassName('tag--selected').length > 0;

        if (hasSelectedTags) {
            tagElement.classList.add('tags--has-selected-tags');
        } else {
            tagElement.classList.remove('tags--has-selected-tags');
        }
    });
}
window.resetTagsElementVisibility = resetTagsElementVisibility;

function resetTagVisibility(tagGroup) {
    const tags = tagGroup.querySelectorAll(
        '.tags--unselected .tag:not(.tag--selected):not(.tag--filtered)'
    );

    tags.forEach((tag, idx) => {
        if (idx > whTagGroupLimit) {
            tag.classList.add('tag--hidden');
        } else {
            tag.classList.remove('tag--hidden');
        }
    });

    if (tags.length === 0) {
        tagGroup.classList.add('tag-group--empty');
    } else {
        tagGroup.classList.remove('tag-group--empty');
    }
}

function filterTags(tagBoxId, textInput, collapsable) {
    const tagBox = document.getElementById(tagBoxId);
    const tagGroups = tagBox.querySelectorAll('.tags--unselected .tag-group');
    const unselectedTags = tagBox.querySelectorAll('.tags--unselected .tag');

    if (textInput.value && textInput.value !== '') {
        let tagSearch = textInput.value.toLowerCase();

        unselectedTags.forEach(tag => {
            let tagLabel = tag.attributes['data-label'].value.toLowerCase();
            if (!tagLabel.includes(tagSearch)) {
                tag.classList.add('tag--filtered');
            } else {
                tag.classList.remove('tag--filtered');
            }
        });

        if (collapsable) {
            tagBox.classList.remove('tags-container--collapsed');
        }
    } else {
        // search box is empty so remove all filtered classes
        unselectedTags.forEach(tag => tag.classList.remove('tag--filtered'));

        if (collapsable) {
            tagBox.classList.add('tags-container--collapsed');
        }
    }

    // reset visibility
    tagGroups.forEach(group => resetTagVisibility(group));
}
window.filterTags = filterTags;

function handleTagChange(tagBox, queryParamKey) {
    if (tagBox.focusedTag) {
        // apply skeleton class to all company cards AND all tags
        const companyCards = Array.from(
            document.getElementsByClassName('companies__company company-card')
        );

        companyCards.forEach(card => {
            card.classList.add('skeleton');
            Array.from(card.getElementsByClassName('tag')).forEach(tag =>
                tag.classList.add('tag--skeleton')
            );
        });

        const tagElement = tagBox.focusedTag;
        let tagQueryId = tagBox.focusedTagQueryId;

        tagQueryId =
            tagQueryId.endsWith(':size') ||
            tagQueryId.endsWith(':location') ||
            tagQueryId.endsWith(':tag')
                ? tagQueryId.split(':')[0]
                : tagQueryId;
        const adding = tagElement.classList.contains('tag--selected');

        queryParamKey = tagElement.getAttribute('data-query-param') || queryParamKey;

        let url = setQueryParam(null, 'interaction', 1);
        url = deleteQueryParam(url, 'page');

        if (tagQueryId.endsWith(':size')) {
            if (adding) {
                return setQueryParam(url, 'size', tagQueryId);
            } else {
                return deleteQueryParam(url, 'size');
            }
        } else {
            if (adding) {
                return addQueryParam(url, queryParamKey, tagQueryId);
            } else {
                return removeQueryParam(url, queryParamKey, tagQueryId);
            }
        }
    }
}
window.handleTagChange = handleTagChange;

function createTagQueryId(slug, type) {
    return slug + ':' + type;
}

const parseSelectedTags = () => new URL(location.href).searchParams.getAll('tag');

const parseSize = () => new URL(location.href).searchParams.get('size');

function createIcon(icon) {
    const svgns = 'http://www.w3.org/2000/svg';
    const xlinkns = 'http://www.w3.org/1999/xlink';
    let svg = document.createElementNS(svgns, 'svg');
    let use = document.createElementNS(svgns, 'use');
    svg.classList.add('icon');
    svg.classList.add('icon--' + icon);
    use.setAttributeNS(xlinkns, 'href', '#' + icon);
    svg.appendChild(use);
    return svg;
}

function createTagGroup(parent, tagGroupType) {
    let tagGroupElement = document.createElement('div');
    let tagGroupInnerElement = document.createElement('ul');
    tagGroupElement.classList.add('tag-group');
    tagGroupElement.classList.add('tag-group--' + tagGroupType);
    tagGroupInnerElement.classList.add('tags');
    tagGroupElement.appendChild(tagGroupInnerElement);
    parent.appendChild(tagGroupElement);
    return tagGroupInnerElement;
}

function createTag({ tag, parent, grandParent, isSelected, hasIcon }, collapsable) {
    let parents = grandParent.getElementsByClassName('tags');
    const tagElement = document.createElement('li');
    const tagSlugClass = 'tag--slug-' + tag.slug;
    const tagTypeClass = 'tag--type-' + tag.type;
    const tagQueryId = createTagQueryId(tag.slug, tag.type);

    tagElement.setAttribute('data-label', tag.label);
    tagElement.classList.add('tag');
    tagElement.classList.add(tagTypeClass);
    tagElement.classList.add(tagSlugClass);

    if (tag.subtype) {
        tagElement.classList.add('tag--subtype-' + tag.subtype);
    }

    if (tag.attr) {
        tagElement.setAttribute('data-query-param', tag.attr);
    }

    if (isSelected) {
        tagElement.classList.add('tag--selected');
        // add a class to both parents too help with styling
        Array.from(parents).forEach(parent =>
            parent.classList.add('tags--has-selected-tags')
        );
    }

    if (hasIcon) {
        tagElement.appendChild(createIcon('close'));
    }

    tagElement.onclick = function () {
        let elements = grandParent.querySelectorAll(
            '.' + tagSlugClass + '.' + tagTypeClass
        );

        elements.forEach(element => element.classList.toggle('tag--selected'));

        // close the expansion
        if (collapsable) {
            grandParent.classList.add('tags-container--collapsed');
        }

        if (grandParent.onsubmit) {
            grandParent.focusedTagQueryId = tagQueryId;
            grandParent.focusedTag = tagElement;
            grandParent.onsubmit(tagQueryId, tagElement, tag.value, tag.attr);
            grandParent.focusedTagQueryId = grandParent.focusedTag = null;
        }

        resetTagsElementVisibility(grandParent);
    };

    let tagSpan = document.createElement('span');
    tagSpan.innerText = tag.label;
    tagElement.appendChild(tagSpan);
    parent.appendChild(tagElement);
}

const isElementCorrect = (element, label, type) => {
    return (
        element.innerText.toLowerCase() === label.toLowerCase() &&
        element.classList.contains('tag--type-' + type.toLowerCase())
    );
};

/**
 * Clicks on tag which includes provided label in text
 *                   & includes provided type in classlist.
 * Tag is selected from unselected tags area.
 *
 * @param {string} label
 * @param {string} type
 *
 * @example
 *     clickOnTag('Remote Working', 'benefit')
 *     clickOnTag('Scala', 'tech')
 */
const clickOnTag = (label, type) => {
    const UNSELECTED_TAGS = '.tags--unselected';
    const TAG = '.tag';

    const tagElement = Array.from(
        document.querySelector(UNSELECTED_TAGS).querySelectorAll(TAG)
    ).filter(elm => isElementCorrect(elm, label, type))[0];

    if (tagElement) {
        tagElement.click();
    }
};
window.clickOnTag = clickOnTag;

const removeAllChildren = parent => {
    while (parent.firstChild) {
        parent.removeChild(parent.firstChild);
    }
};

// used to imitate Promise API. we could use Promise.resolve() instead,
// but that would schedule redundant async (micro)task
const thenable = data => ({
    then: resolve => resolve(data)
});

export function initTags(
    tagBox,
    tagsCollection,
    forceRender,
    selected,
    collapsable,
    showIcons,
    tagsUrl
) {
    // Initialize tags if 1) element and data are present, and 2) tagBox contains .tags-loading,
    // which means it's waiting for tags to be loaded, or forceRender is passed to indicate
    // that CLJS caller decided that tags collection has changed
    const render = tagBox.querySelector('.tags-loading') || forceRender;

    if ((!tagBox || !tagsCollection || !tagsCollection.length || !render) && !tagsUrl) {
        return;
    }

    selected = selected ? selected : [];

    const selectedTags = parseSelectedTags().concat(selected);
    const sizeValue = parseSize();
    const tagParents = [
        {
            parent: tagBox.querySelector('ul.tags.tags--unselected'),
            groups: true,
            icon: false,
            matchMsg: collapsable
        },
        {
            parent: tagBox.querySelector('ul.tags.tags--selected'),
            groups: false,
            icon: showIcons,
            matchMsg: false
        }
    ];

    let tagGroups = [];

    const fetchTags = () =>
        tagsUrl
            ? fetch(tagsUrl)
                  .then(tags => tags.json())
                  .then(({ tags }) => tags)
            : thenable(tagsCollection);

    fetchTags().then(tagsCollection => {
        // remove any children from parent element such as loading msgs
        tagParents.forEach(({ parent, groups, matchMsg, icon }) => {
            removeAllChildren(parent);

            let currentGroup = groups
                ? null
                : createTagGroup(parent, tagsCollection[0].type);
            let currentType = null;

            tagsCollection.forEach(tag => {
                // this group/type management code relies on tags being sorted by type
                if (groups && currentType != tag.type) {
                    currentType = tag.type;
                    currentGroup = createTagGroup(parent, currentType);
                    tagGroups.push(currentGroup);
                }

                const tagQueryId = createTagQueryId(tag.slug, tag.type);

                createTag(
                    {
                        tag: tag,
                        parent: currentGroup,
                        grandParent: tagBox,
                        isSelected:
                            -1 != selectedTags.indexOf(tagQueryId) ||
                            (tag.type === 'size' && tag.slug === sizeValue),
                        hasIcon: icon
                    },
                    collapsable
                );
            });

            tagGroups.forEach(group => resetTagVisibility(group));

            // add a text span after the tag groups
            if (matchMsg) {
                let noMatchingSpan = document.createElement('span');
                noMatchingSpan.innerText = 'No tags matched the search term!';
                parent.appendChild(noMatchingSpan);
            }
        });
    });
}
window.initTags = initTags;

function invertTag(tag) {
    tag.classList.toggle('tag--inverted');
}
window.invertTag = invertTag;
